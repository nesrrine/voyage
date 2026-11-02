# Guide pas à pas : ouvrir et tester tout le projet

Toutes les commandes se lancent dans un terminal PowerShell, depuis le dossier du projet :

```
cd C:\Users\USER\Desktop\Projet-Voyages-Affaires\voyages-affaires
```

---

## Étape 0 — Préparer le PC (8 Go de RAM)

1. Fermez Chrome, Edge et les applications lourdes.
2. Ouvrez **Docker Desktop** et attendez **« Engine running »** en bas à gauche.
   N'installez pas de mise à jour de Docker pendant les tests.
3. Si Docker affiche *« An unexpected error occurred … .sock: The file cannot be accessed by the system »* :
   clic droit sur `C:\Users\USER\Desktop\Projet-Voyages-Affaires\reparer-docker.ps1` → **Exécuter avec PowerShell**,
   puis attendez « Engine running ».

---

## Étape 1 — Tests automatisés (sans Docker)

**Backend (91 tests)** — résultat attendu : `BUILD SUCCESS`

```
cd backend
C:\maven-mvnd-1.0.3-windows-amd64\bin\mvnd.cmd test
cd ..
```

(Dans IntelliJ : clic droit sur le dossier `backend` → *Run 'All Tests'*.)

**Frontend (38 tests)** — résultat attendu : `TOTAL: 38 SUCCESS`

```
cd frontend
npm install
npm run test:ci
cd ..
```

---

## Étape 2 — Lancer toute l'application dans Docker

**2.1 Construire les jars** (à refaire après chaque modification du code Java) :

```
cd backend
C:\maven-mvnd-1.0.3-windows-amd64\bin\mvnd.cmd -DskipTests package
cd ..
```

**2.2 Construire les images et démarrer les 10 conteneurs** (5 à 10 minutes la première fois) :

```
docker compose up -d --build
```

**2.3 Vérifier que tout tourne** (attendre 2 à 3 minutes après le démarrage) :

```
docker compose ps
```

Les services doivent être `running`, la plupart `(healthy)`. Dans Docker Desktop → **Containers**, vous voyez
le groupe **voyages-affaires** avec ses 10 conteneurs en vert.

---

## Étape 3 — Ouvrir et tester chaque partie

| Ouvrir dans le navigateur | Ce qu'il faut vérifier |
|---|---|
| **http://localhost:4200** | L'application. Cliquez sur un compte de démonstration (ex. « Employé Amine »), puis **Se connecter**. Testez : *Planning des voyages*, le détail du voyage « Salon VivaTech » (partenaires trouvés + scores), *Programmes*, *Invitations de voyage*, *Forum* (Like / Dislike). |
| **http://localhost:8761** | **Eureka** : 5 services inscrits (API-GATEWAY, USER-SERVICE, VOYAGE-SERVICE, FORUM-SERVICE, AI-SERVICE). |
| **http://localhost:9090/targets** | **Prometheus** : les 6 cibles du job `microservices` sont **UP** (en vert). |
| **http://localhost:9090/alerts** | Les 5 règles d'alerte sont chargées (vertes = aucun problème). |
| **http://localhost:3000** | **Grafana** — identifiant `admin`, mot de passe `admin` (« Skip » pour ne pas le changer). Le dashboard **Voyages & Affaires - Microservices** s'ouvre et affiche les courbes. |

Comptes de démonstration : `admin@voyages-affaires.com / admin123`, `contact@techsoft.tn / password123`,
`amine@techsoft.tn / password123`, `karim@finconseil.fr / password123`.

---

## Étape 4 — Démonstrations pour la soutenance

**A. Une métrique métier qui bouge**
1. Connectez-vous avec `sarra@techsoft.tn / password123`.
2. *Nouveau voyage* : Paris, dans 3 semaines, objet « Salon professionnel », domaine recherché « Énergie ».
3. Le message indique les partenaires trouvés ; dans Grafana, le panneau **Invitations de voyage (matching)** augmente.

**B. Panne d'un microservice : alerte + circuit breaker**
1. Arrêtez le forum :
   ```
   docker compose stop forum-service
   ```
2. Connectez-vous en **admin** et ouvrez le **Tableau de bord** : il reste affiché (publications = 0)
   grâce au circuit breaker.
3. Après ~1 minute : l'alerte **MicroserviceIndisponible** devient rouge dans http://localhost:9090/alerts et dans
   le panneau **Alertes en cours** de Grafana ; « Services disponibles » passe à 5.
4. Redémarrez-le :
   ```
   docker compose start forum-service
   ```

**C. Sécurité à la Gateway**
Ouvrez http://localhost:4200/api/voyages dans le navigateur : réponse **401** venant de `api-gateway`
(pas de jeton = la requête n'atteint jamais le microservice).

**Arrêter l'application** : `docker compose down` (ajoutez `-v` pour aussi effacer la base MySQL).

---

## Étape 5 — SonarQube (qualité du code)

Arrêtez d'abord l'application (`docker compose down`) : SonarQube a besoin d'environ 2 Go de RAM.

1. Démarrer SonarQube :
   ```
   docker compose --profile qualite up -d sonarqube
   ```
2. Ouvrir **http://localhost:9000** (attendre 2 minutes) → `admin` / `admin`, puis choisir un nouveau mot de passe.
3. *My Account → Security → Generate Token* (type *Global Analysis Token*) et copier le jeton.
4. Analyse du **backend** (remplacez `VOTRE_JETON` ; sous PowerShell, les guillemets autour de chaque `-D...` sont obligatoires) :
   ```
   cd backend
   C:\maven-mvnd-1.0.3-windows-amd64\bin\mvnd.cmd verify org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar "-Dsonar.host.url=http://localhost:9000" "-Dsonar.token=VOTRE_JETON" "-Dsonar.projectKey=voyages-affaires-backend"
   cd ..
   ```
5. Analyse du **frontend** (après `npm run test:ci`, qui produit la couverture) :
   ```
   cd frontend
   docker run --rm -e SONAR_HOST_URL=http://host.docker.internal:9000 -e SONAR_TOKEN=VOTRE_JETON -v "${PWD}:/usr/src" sonarsource/sonar-scanner-cli:11
   cd ..
   ```
6. Dans SonarQube : les projets **Voyages & Affaires - Backend** et **- Frontend** affichent bugs, vulnérabilités,
   code smells et **couverture**.

---

## Étape 6 — Jenkins (pipelines CI/CD)

1. Démarrer Jenkins (et SonarQube si besoin) :
   ```
   docker compose --profile ci up -d --build jenkins
   ```
2. Ouvrir **http://localhost:8090**. Mot de passe initial :
   ```
   docker compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```
   Choisir **« Install suggested plugins »** puis créer votre compte (les plugins du projet sont déjà installés).
3. **Credentials** (*Administrer Jenkins → Credentials → System → Global → Add Credentials*) :
   - `dockerhub` : *Username with password* = compte Docker Hub + *Access Token* (hub.docker.com → Account settings → Personal access tokens) ;
   - `kubeconfig` : *Secret file* = fichier `/etc/kubernetes/admin.conf` du maître kubeadm (étape 7) ;
   - un *Secret text* contenant le jeton SonarQube.
4. **Serveur SonarQube** (*Administrer Jenkins → System → SonarQube servers*) : nom **`SonarQube`**,
   URL `http://host.docker.internal:9000`, jeton = le *Secret text*.
5. **Créer les 2 jobs** (*Nouveau Item → Pipeline*) :
   - `voyages-backend` : *Pipeline script from SCM* → Git `https://github.com/nesrrine/voyage.git`, branche `*/main`,
     Script Path **`backend/Jenkinsfile`**, cocher **GitHub hook trigger for GITScm polling** ;
   - `voyages-frontend` : pareil avec Script Path **`frontend/Jenkinsfile`**.
6. Lancer chaque job une première fois avec **Build Now** : les étapes (tests, SonarQube, Docker, déploiement)
   s'affichent dans *Stage View*. Décochez le paramètre `DEPLOYER` tant que le cluster n'est pas prêt.
7. **Webhook GitHub** (déclenchement automatique à chaque push) :
   - exposer Jenkins sur Internet : installer ngrok puis `ngrok http 8090` → copier l'URL `https://xxxx.ngrok-free.app` ;
   - GitHub → dépôt `nesrrine/voyage` → *Settings → Webhooks → Add webhook* :
     Payload URL `https://xxxx.ngrok-free.app/github-webhook/`, Content type `application/json`, *Just the push event* ;
   - **Test** : modifiez un fichier dans `frontend/`, faites `git commit` puis `git push` → le job `voyages-frontend` démarre tout seul.
8. **Webhook SonarQube** (pour l'étape *Quality Gate*) : SonarQube → *Administration → Configuration → Webhooks →
   Create* : URL `http://host.docker.internal:8090/sonarqube-webhook/`.

---

## Étape 7 — Kubernetes (cluster kubeadm sur vos VMs)

1. Sur chaque VM Ubuntu : installer `containerd`, `kubeadm`, `kubelet`, `kubectl`, désactiver le swap.
2. Sur le maître : `sudo kubeadm init --pod-network-cidr=10.244.0.0/16`, copier `/etc/kubernetes/admin.conf`
   dans `~/.kube/config`, installer le réseau (ex. Flannel), puis joindre les nœuds avec la commande `kubeadm join`
   affichée. Vérifier : `kubectl get nodes` → tous `Ready`.
3. Les images doivent être sur Docker Hub (poussées par Jenkins, étape 6).
4. Sur le maître, avec le projet cloné :
   ```
   git clone https://github.com/nesrrine/voyage.git && cd voyage
   sh k8s/deploy.sh nesrrine latest
   kubectl -n voyages-affaires get pods
   ```
   Tous les pods doivent être `Running` et `1/1`.
5. Ouvrir (IP d'un nœud) : `:30080` application, `:30761` Eureka, `:30090` Prometheus, `:30030` Grafana.
6. Démo de panne en Kubernetes :
   ```
   kubectl -n voyages-affaires scale deployment/forum-service --replicas=0
   kubectl -n voyages-affaires scale deployment/forum-service --replicas=1
   ```

Ensuite, chaque push sur GitHub reconstruit, analyse et redéploie automatiquement la partie modifiée.
