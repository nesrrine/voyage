# Voyages & Affaires — Sprint 3 (DevOps)

Chaîne complète : **push GitHub → webhook → Jenkins (tests, SonarQube, images Docker) → Docker Hub →
déploiement sur le cluster kubeadm → supervision Prometheus / Grafana avec alertes.**

```
 git push ──► GitHub ──webhook──► Jenkins ──► 1. build + tests (Maven / Karma, couverture)
                                          ├─► 2. analyse SonarQube + Quality Gate
                                          ├─► 3. docker build + push  ──► Docker Hub (nesrrine/va-*)
                                          └─► 4. k8s/deploy.sh ──► cluster kubeadm ──► Prometheus ──► Grafana
```

## Fichiers du sprint

| Élément | Fichiers |
|---|---|
| Pipelines Jenkins | `backend/Jenkinsfile`, `frontend/Jenkinsfile`, image Jenkins `ci/jenkins/` |
| Images Docker | `backend/<service>/Dockerfile` (6 microservices), `frontend/Dockerfile` + `frontend/nginx.conf` |
| Stack locale | `docker-compose.yml` (MySQL, 6 services, frontend, Prometheus, Grafana, + SonarQube / Jenkins en option) |
| Kubernetes | `k8s/*.yaml` (générés par `k8s/generer_manifests.py`), `k8s/monitoring/`, `k8s/deploy.sh` |
| Qualité | `frontend/sonar-project.properties`, JaCoCo dans `backend/pom.xml`, `frontend/karma.conf.js` (lcov) |
| Supervision | `monitoring/prometheus/` (scrape + `alert-rules.yml`), `monitoring/grafana/` (datasource + dashboard) |

## 1. Tester toute la stack en local (Docker Compose)

```bash
cd backend && mvn -DskipTests package && cd ..
```

```bash
docker compose up -d --build
```

| URL | Service |
|---|---|
| http://localhost:4200 | Application (nginx → `/api` → API Gateway) |
| http://localhost:8761 | Eureka (6 services enregistrés) |
| http://localhost:9090 | Prometheus (Status > Targets, Alerts) |
| http://localhost:3000 | Grafana (admin / admin) — dashboard « Voyages & Affaires - Microservices » |

Les données sont dans MySQL (une base par microservice, créées automatiquement au démarrage).

## 2. Jenkins

### Installation (image fournie)

```bash
docker compose --profile ci --profile qualite up -d jenkins sonarqube
```

Jenkins : http://localhost:8090 (mot de passe initial : `docker compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword`).
Les plugins nécessaires (`ci/jenkins/plugins.txt`) sont déjà installés. Maven, Node/Chromium, sonar-scanner et
kubectl sont lancés dans des conteneurs Docker par les pipelines : rien d'autre à installer sur Jenkins.

### Credentials (Administrer Jenkins > Credentials)

| ID | Type | Contenu |
|---|---|---|
| `dockerhub` | Username with password | compte Docker Hub + *access token* Docker Hub |
| `kubeconfig` | Secret file | fichier `~/.kube/config` du nœud maître kubeadm (`/etc/kubernetes/admin.conf`) |
| (SonarQube) | Secret text | jeton généré dans SonarQube (My Account > Security) |

Dans le dépôt, le compte Docker Hub cible est `REGISTRY = 'nesrrine'` (en haut des deux Jenkinsfiles) : à adapter si
votre compte Docker Hub porte un autre nom.

### Serveur SonarQube (Administrer Jenkins > System > SonarQube servers)

- Nom : **`SonarQube`** (nom utilisé par `withSonarQubeEnv('SonarQube')`)
- URL : `http://<IP-de-la-machine>:9000` (accessible depuis les conteneurs de build, donc pas `localhost`)
- Jeton : le credential « Secret text » ci-dessus

### Les deux jobs

Nouveau Item → **Pipeline** → « Pipeline script from SCM » → Git `https://github.com/nesrrine/voyage.git`, branche `main` :

| Job | Script Path | Déclencheur |
|---|---|---|
| `voyages-backend` | `backend/Jenkinsfile` | ☑ GitHub hook trigger for GITScm polling |
| `voyages-frontend` | `frontend/Jenkinsfile` | ☑ GitHub hook trigger for GITScm polling |

Chaque pipeline ne travaille que si le push touche sa partie (`changeset`) ; « Build Now » lance toujours tout.
Lancer **une première fois chaque job à la main** pour que Jenkins enregistre le déclencheur.

### Webhooks

1. **GitHub → Jenkins** (dépôt `nesrrine/voyage` > Settings > Webhooks > Add webhook) :
   Payload URL `http://<adresse-publique-de-jenkins>/github-webhook/`, Content type `application/json`, événement *push*.
   Si Jenkins tourne sur votre PC, exposez-le avec un tunnel (`ngrok http 8090`) et utilisez l'URL fournie.
2. **SonarQube → Jenkins** (SonarQube > Administration > Configuration > Webhooks) :
   `http://<IP-de-jenkins>:8090/sonarqube-webhook/` — indispensable pour l'étape *Quality Gate*.

## 3. Cluster kubeadm

Prérequis : un cluster kubeadm fonctionnel (1 maître + nœuds, CNI installé, `kubectl get nodes` → `Ready`).

```bash
./k8s/deploy.sh nesrrine latest
```

Le script crée le namespace `voyages-affaires`, la configuration, MySQL (volume `hostPath` car kubeadm n'a pas de
StorageClass par défaut), les 6 microservices, le frontend, Prometheus et Grafana, puis attend que tout soit prêt.
Jenkins l'appelle avec le numéro de build comme tag (`deploy.sh nesrrine 42 backend`).

| Accès (IP d'un nœud) | Service |
|---|---|
| `:30080` | Application |
| `:30761` | Eureka |
| `:30090` | Prometheus |
| `:30030` | Grafana |

Points d'excellence :
- **Disponibilité** : *rolling update* sans interruption (`maxUnavailable: 0`), sondes `startup` / `liveness` /
  `readiness` sur les endpoints Actuator, `initContainer` qui attend MySQL.
- **Optimisation** : requêtes / limites CPU et mémoire sur chaque pod, JVM calée sur la limite (`MaxRAMPercentage`),
  images Alpine non root, cache Maven partagé entre les builds, pipelines déclenchés seulement si la partie change.
- **Mise à l'échelle** : HPA sur la gateway et le voyage-service (`k8s/20-autoscaling.yaml`, nécessite metrics-server).
- **Retour arrière** : chaque build pousse une image taguée par son numéro (`kubectl -n voyages-affaires rollout undo deployment/<service>`).

## 4. Supervision et alertes

Chaque microservice expose `/actuator/prometheus`. Prometheus les découvre (cibles statiques en Docker, annotations
`prometheus.io/*` des pods en Kubernetes) et Grafana affiche le dashboard provisionné automatiquement :

- services disponibles, requêtes/s, taux d'erreurs 5xx, latence p95 (global et par service), codes HTTP ;
- mémoire JVM, CPU, threads ;
- état des **circuit breakers** Resilience4j, taux de succès du **cache** Caffeine ;
- métrique **métier** : invitations de voyage créées par le matching et score moyen ;
- tableau des **alertes en cours**.

Règles d'alerte (`monitoring/prometheus/alert-rules.yml`) :

| Alerte | Condition |
|---|---|
| `MicroserviceIndisponible` | un service ne répond plus depuis 1 min |
| `TauxErreurs5xxEleve` | plus de 5 % d'erreurs serveur pendant 5 min |
| `LatenceElevee` | p95 > 1 s pendant 5 min (hors appels IA) |
| `MemoireJvmSaturee` | tas Java utilisé à plus de 90 % |
| `CircuitBreakerOuvert` | un service appelé est en panne (circuit ouvert) |

Démonstration : arrêter un service (`kubectl -n voyages-affaires scale deployment/forum-service --replicas=0`) →
l'alerte passe à *firing* dans Prometheus et dans Grafana, le tableau de bord de l'application reste disponible grâce
au circuit breaker.

## Couverture de la grille Sprint 3

| Critère | Points | Réalisation |
|---|---|---|
| Jenkins & webhooks (Front & Back) | 3 | 2 pipelines déclenchés par webhook GitHub (`githubPush()`), un par partie |
| Docker : image back-end | 0.75 | 6 images (`nesrrine/va-*`) construites et poussées par le pipeline backend |
| Docker : image front-end | 0.75 | image multi-étapes `nesrrine/va-frontend` poussée par le pipeline frontend |
| Orchestration kubeadm | 2.5 | `k8s/` + `deploy.sh`, appelé par les deux pipelines |
| SonarQube | 1.5 | analyse back (JaCoCo) et front (lcov) + Quality Gate bloquante dans Jenkins |
| Grafana via Prometheus | 1.5 | dashboard provisionné, 16 panneaux |
| Excellence (individuel) | — | 5 alertes Prometheus, métriques métier, HPA, sondes, rolling update, images optimisées, circuit breaker supervisé |
