# Voyages & Affaires

Application web qui met en relation les employés de différentes entreprises partant en voyage d'affaires
vers les mêmes destinations, aux mêmes dates, et dont les domaines d'activité sont complémentaires.

- **Backend** : architecture **microservices** Spring Boot 3.3 / Spring Cloud 2023 (Java 17) :
  **Eureka** (annuaire), **Spring Cloud Gateway** (point d'entrée sécurisé par JWT), **OpenFeign** (appels entre
  services) protégé par un **circuit breaker Resilience4j**, cache **Caffeine**, Spring Security + JWT,
  Spring Data JPA, une base H2 par service (MySQL possible)
- **Frontend** : Angular 19 (composants standalone, signals), Angular Material, Chart.js (ng2-charts)
- **IA** : API Claude d'Anthropic (SDK Java officiel)

> **Sprint 3 (DevOps)** : pipelines Jenkins, images Docker, déploiement Kubernetes (kubeadm), SonarQube,
> Prometheus / Grafana et alertes — voir **[DEVOPS.md](DEVOPS.md)**. Guide pas à pas pour tout ouvrir et tester : **[TESTER.md](TESTER.md)**. Stack complète en local : `docker compose up -d --build`.

## Architecture

```
                        Navigateur (Angular, port 4200)
                                     │  http://localhost:8080/api/**
                                     ▼
                        ┌──────────────────────────┐
                        │   api-gateway  (8080)    │  routage + CORS
                        └────────────┬─────────────┘
                 lb://nom-du-service │ (adresses lues dans Eureka)
     ┌───────────────┬───────────────┼───────────────┬────────────────┐
     ▼               ▼               ▼               ▼                │
┌──────────┐   ┌────────────┐  ┌────────────┐  ┌────────────┐         │
│  user-   │◄─►│  voyage-   │◄─│  forum-    │  │    ai-     │         │
│ service  │   │  service   │  │  service   │  │  service   │──► API Claude
│  8081    │   │   8082     │  │   8083     │  │   8084     │
│ userdb   │   │ voyagedb   │  │  forumdb   │  │ (sans base)│
└────┬─────┘   └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
     └───────────────┴───────┬───────┴───────────────┘
                             ▼  enregistrement / découverte
                  ┌──────────────────────────┐
                  │ discovery-server (8761)  │  Netflix Eureka
                  └──────────────────────────┘
```

| Module Maven | Port | Rôle | Module du sujet |
|---|---|---|---|
| `discovery-server` | 8761 | Serveur **Eureka** : chaque service s'y enregistre | infrastructure |
| `api-gateway` | 8080 | **Spring Cloud Gateway** : point d'entrée unique, **vérifie le JWT et les rôles**, route `/api/**` vers le bon service, CORS | infrastructure |
| `user-service` | 8081 | Inscription entreprise, authentification (émet le JWT), autorisation, invitations des employés, comptes, administration | **Gestion des utilisateurs** |
| `voyage-service` | 8082 | CRUD voyages, **algorithme de matching**, invitations de voyage, programmes, référentiel des villes, statistiques | **Gestion des voyages** |
| `forum-service` | 8083 | Publications, commentaires, **Like / Dislike** | **Forum** |
| `ai-service` | 8084 | Les 5 fonctionnalités IA (API Claude) | fonctionnalités avancées |
| `common` | – | Bibliothèque partagée : JWT, filtre de sécurité, exceptions, DTO, configuration Feign | – |

### Routage de la gateway

| Chemin | Service |
|---|---|
| `/api/auth/**`, `/api/account/**`, `/api/invitations/**`, `/api/entreprise/**`, `/api/admin/**`, `/api/users/**` | user-service |
| `/api/voyages/**`, `/api/matchs/**`, `/api/meta/**`, `/api/stats/**` | voyage-service |
| `/api/forum/**` | forum-service |
| `/api/ai/**` | ai-service |

### Communication entre microservices (OpenFeign + Eureka)

Les appels utilisent le **nom** du service (`@FeignClient(name = "user-service")`) : l'adresse réelle est trouvée
dans Eureka. Le jeton JWT de l'utilisateur est **relayé** automatiquement (`common/feign/FeignConfig`), et les
erreurs du service appelé (403, 404...) sont converties en `ApiException` avec le même statut.

| Appelant → appelé | Quand |
|---|---|
| voyage-service → user-service `GET /api/users/{id}` | une entreprise planifie le voyage d'un de ses employés |
| voyage-service → user-service `GET /api/users/stats` | tableau de bord (employés, invitations, entreprises par domaine) |
| voyage-service → forum-service `GET /api/forum/stats` | tableau de bord admin (publications par catégorie) |
| user-service → voyage-service `PUT /api/voyages/employes/{id}/actif` | un employé est désactivé / réactivé : ses voyages sortent du matching |
| ai-service → voyage-service / forum-service | contexte de l'IA (voyages, invitations, programmes, discussion du forum) |

Si un service appelé est arrêté, l'appelant continue de fonctionner (valeur de repli, avertissement dans les logs).

### Sécurité à deux niveaux (JWT)

**Niveau 1 — API Gateway** (`api-gateway/.../security/JwtGatewayFilter`) : avant tout routage, la gateway
vérifie la signature et l'expiration du jeton, puis le rôle requis par le chemin :

| Chemin | Accès à la gateway |
|---|---|
| `POST /api/auth/login`, `/register-*`, `GET /api/auth/invitation/**`, `GET /api/meta/**` | public |
| `/api/admin/**` | ADMIN |
| `/api/invitations/**`, `/api/entreprise/**` | ENTREPRISE |
| `/api/matchs/**` | EMPLOYE, ENTREPRISE |
| tout le reste de `/api/**` | utilisateur connecté |

Une requête refusée ne parvient jamais au microservice (401 / 403 avec `"source":"api-gateway"`). La gateway
transmet l'identité vérifiée (`X-User-Id`, `X-User-Role`) et supprime ces en-têtes s'ils viennent du client.

**Niveau 2 — chaque microservice** (`common/.../security`) : chaque service revérifie lui-même le jeton
(un appel direct sur son port, sans passer par la gateway, est refusé), applique ses règles d'URL et
`@PreAuthorize`, puis les règles d'appartenance dans les services (un employé ne modifie que ses voyages, une
entreprise que ceux de ses employés, seul le destinataire répond à une invitation de voyage, etc.).

- Le **user-service** vérifie le mot de passe (BCrypt) et signe le **JWT** (identifiant, rôle, nom, poste, entreprise).
- Après une modification du profil ou de l'entreprise, il renvoie un **nouveau jeton** (remplacé par le frontend).

### Résilience et performance

- **Circuit breaker Resilience4j** sur tous les clients OpenFeign, avec une `FallbackFactory` par client :
  après 5 échecs (service arrêté, erreur 5xx, délai > 10 s), le circuit s'ouvre et la réponse de repli est
  immédiate ; il se referme tout seul quand le service revient. Les refus métier (403, 404) ne comptent pas
  comme des pannes (`common/.../feign/PanneDistante`) et sont propagés tels quels.
  *Mesure réelle : forum-service arrêté → le tableau de bord admin répond toujours (publications = 0) ;
  les 4 premiers appels prennent ~2 s, puis le circuit s'ouvre et les réponses tombent à ~30 ms.*
- **Cache Caffeine** (voyage-service) : référentiel des villes et programmes publics mis en cache
  (`ReferentielCache`), invalidés par `@CacheEvict` à chaque création, modification ou suppression de voyage.

### Une base de données par service

Chaque microservice possède sa propre base (`backend/<service>/data/`). Un voyage référence son employé par
`employeId` et garde une copie des informations utiles (nom, poste, entreprise, domaine) ; une publication garde
de même `auteurId` et le nom de l'auteur. Le jeu de démonstration du user-service crée les employés avec des
identifiants fixes (6 à 11) repris par le voyage-service et le forum-service : pour repartir de zéro,
**supprimer les trois dossiers `data/` ensemble**.

## Couverture de la grille d'évaluation (Sprint 2)

| Critère | Où le trouver |
|---|---|
| **AA1** Logique métier avancée et relations entre entités (Module 2) | `voyage-service` : `MatchingService` (score multicritère, Haversine), relations `Voyage` ↔ `MatchVoyage` (source / cible), référentiel `Ville`, cycle de vie des invitations de voyage (`MatchService`), statistiques (`StatsService`). Angular : formulaire dynamique (autocomplétion des villes → coordonnées GPS, périmètre, puces de domaines), détail du score, graphiques |
| **AA2** OpenFeign et routage par l'API Gateway | Routes de `api-gateway/application.yml` (`lb://` + Eureka) ; clients `@FeignClient` des 3 services avec relais du JWT (`common/feign/FeignConfig`) ; tableau « Communication entre microservices » ci-dessus |
| **AA3** Sécurité JWT, niveau Gateway (1 pt) | `JwtGatewayFilter` + `RegleAcces` (401 / 403 avant routage) ; `JwtGatewayFilterTest` |
| **AA3** Sécurité JWT, niveau microservices (1 pt) | `common/security` (filtre JWT dans chaque service), `SecurityConfig` de chaque service, `@PreAuthorize`, contrôles d'appartenance ; tests `*IntegrationTest` |
| **AA4** Tests backend et frontend | 91 tests JUnit / Mockito / MockMvc et 38 tests Jasmine / Karma (section Tests) |
| **C4** Git et standards | Dépôt Git : un commit par module puis par fonctionnalité, messages au format *Conventional Commits* (`feat(...)`, `test(...)`, `docs(...)`), README à jour |
| **C5** Innovation / complexité technique | Intégration d'une API externe d'IA (Claude) dans un microservice dédié ; circuit breaker Resilience4j avec repli ; cache Caffeine ; sécurité JWT à deux niveaux |

## Lancer le projet

### 1. Compiler (une fois, et après chaque modification du backend)

```bash
cd backend
mvn -DskipTests package
```

### 2. Démarrer les microservices

Script Windows (ouvre une fenêtre par service, dans le bon ordre) :

```bash
powershell -ExecutionPolicy Bypass -File demarrer-microservices.ps1
```

Ou à la main, **dans cet ordre** (chaque commande dans son terminal, depuis le dossier du module) :

1. `discovery-server` → attendre http://localhost:8761
2. `user-service`, `voyage-service`, `forum-service`, `ai-service`
3. `api-gateway`

```bash
cd discovery-server && java -jar target/discovery-server-1.0.0.jar
```

(Dans IntelliJ : ouvrir le dossier `backend`, puis lancer les classes `*Application` dans le même ordre.)

Le tableau de bord Eureka (http://localhost:8761) doit afficher les 5 services : API-GATEWAY, USER-SERVICE,
VOYAGE-SERVICE, FORUM-SERVICE, AI-SERVICE.

### 3. Frontend (port 4200)

```bash
cd frontend
npm install
npm start
```

Puis ouvrir http://localhost:4200. Le frontend appelle `/api`, redirigé vers la gateway (`http://localhost:8080`) par le proxy de `ng serve` (`proxy.conf.json`) ; en Docker et Kubernetes, c'est nginx qui fait ce relais.

> Angular 19 supporte officiellement Node 18 / 20 / 22. Avec Node 24, le cache de build plantait :
> il est désactivé dans `angular.json` (`"cli": { "cache": { "enabled": false } }`), ce qui suffit.

Consoles H2 : http://localhost:8081/h2-console (users), 8082 (voyages), 8083 (forum) — JDBC URL
`jdbc:h2:file:./data/userdb` (ou `voyagedb`, `forumdb`), utilisateur `sa`.
Pour **MySQL** : dans le `application.yml` de chaque service, remplacer l'URL H2 par l'URL MySQL en commentaire.

## Modules et fonctionnalités

| Module | Fonctionnalités | Microservice | Frontend |
|---|---|---|---|
| **Gestion des utilisateurs** | Inscription entreprise, authentification JWT, autorisation par rôles (ADMIN / ENTREPRISE / EMPLOYE), invitations des employés, inscription employé par lien, gestion de compte, gestion des employés, administration | `user-service` | `pages/auth/*`, `pages/users/*` |
| **Gestion des voyages** | CRUD voyages (par l'employé ou par son entreprise), **algorithme de matching** (domaine d'activité + dates + destination & périmètre), invitations de voyage (accepter / refuser → mise en contact), consultation des programmes avec filtres, statistiques | `voyage-service` | `pages/voyages/*`, `pages/dashboard/*` |
| **Forum** | Publications, catégories, recherche, tri (récents / populaires / commentés), commentaires, **Like / Dislike** | `forum-service` | `pages/forum/*` |

## Algorithme de matching (`voyage-service` → `MatchingService`)

Lors de l'ajout ou de la modification d'un voyage, l'algorithme recherche les voyages **d'autres entreprises** :

1. **Dates** : les périodes doivent se chevaucher d'au moins un jour.
2. **Destination & périmètre** : distance à vol d'oiseau (formule de Haversine) ≤ le plus grand des deux périmètres (en km).
3. **Domaine d'activité** : 100 % si intérêt mutuel, 70 % si l'un recherche le domaine de l'autre,
   50 % si même domaine (ou aucun domaine précisé), sinon rejeté.

Score final sur 100 = `35 % domaine + 30 % chevauchement des dates + 25 % proximité + 10 % même objet`.
Si le score atteint le seuil (`app.matching.seuil: 40`), une **invitation de voyage** est envoyée au partenaire.
Quand il l'accepte, les deux voyageurs voient leurs coordonnées (email, téléphone).

## Fonctionnalités d'intelligence artificielle (`ai-service`, API Claude)

L'ai-service appelle l'API Claude d'Anthropic (modèle `claude-opus-5-5`) via le SDK Java officiel
(`com.anthropic:anthropic-java`). La clé API reste côté serveur : le navigateur n'y a jamais accès.

| # | Fonctionnalité | Où dans l'application | Endpoint |
|---|---|---|---|
| 1 | **Assistant IA de voyage** : chatbot qui connaît les voyages de l'utilisateur et les programmes publics | Bouton flottant « Assistant IA » sur toutes les pages | `POST /api/ai/chat` |
| 2 | **Analyse d'une rencontre** : pourquoi deux voyageurs ont intérêt à se rencontrer + message de premier contact | Bouton « Analyse IA de la rencontre » sur chaque partenaire trouvé | `GET /api/ai/matchs/{id}/explication` |
| 3 | **Programme de voyage généré** : programme jour par jour + conseils pour la destination | Formulaire de voyage, bouton « Générer le programme avec l'IA » | `POST /api/ai/programme` |
| 4 | **Forum intelligent** : résumé des discussions + modération automatique avant publication | Détail d'une publication / fenêtre « Nouvelle publication » | `GET /api/ai/forum/posts/{id}/resume`, `POST /api/ai/forum/moderation` |
| 5 | **Recommandations** adaptées au rôle (employé, entreprise, admin) | Tableau de bord, carte « Recommandations de l'IA » | `GET /api/ai/recommandations` |

### Activer l'IA

1. Créer une clé API sur https://console.anthropic.com (usage payant).
2. La définir comme variable d'environnement (Windows, une seule fois), puis **rouvrir le terminal / IntelliJ** :
   ```bash
   setx ANTHROPIC_API_KEY "votre-cle"
   ```
3. Redémarrer l'ai-service. Le log affiche `IA activee avec le modele claude-opus-5-5`.

Sans clé, l'application fonctionne normalement : les boutons IA affichent « IA non configurée » et la
publication sur le forum n'est pas modérée. Ne jamais écrire la clé dans un fichier de configuration.

> La modération du forum est vérifiée dans la fenêtre de publication (côté Angular) ; pour une
> protection stricte, le forum-service devrait aussi appeler l'ai-service avant d'enregistrer.

## Comptes de démonstration

| Rôle | Email | Mot de passe |
|---|---|---|
| Administrateur | admin@voyages-affaires.com | admin123 |
| Entreprise (TechSoft, informatique) | contact@techsoft.tn | password123 |
| Employé TechSoft | amine@techsoft.tn / sarra@techsoft.tn | password123 |
| Employé GreenPower (énergie) | youssef@greenpower.tn | password123 |
| Employé FinConseil (finance) | claire@finconseil.fr / karim@finconseil.fr | password123 |
| Employé MedTrade (commerce) | nadia@medtrade.ma | password123 |

La page de connexion propose des boutons pour remplir ces comptes automatiquement.

## Tests

**129 tests automatisés** : 91 côté backend (JUnit 5, Mockito, AssertJ, MockMvc) et 38 côté frontend (Jasmine, Karma).

```bash
cd backend && mvn test
```

```bash
cd frontend && npm run test:ci
```

Côté backend, Eureka est désactivé et chaque service utilise une base H2 en mémoire. Les appels OpenFeign
et l'API Claude sont simulés : aucun appel réseau, rien de payant.

| Service | Classe de test | Ce qui est vérifié |
|---|---|---|
| api-gateway | `JwtGatewayFilterTest` | 1er niveau de sécurité : accès publics, 401 sans jeton / jeton falsifié / expiré, 403 par rôle, transmission de l'identité, en-têtes usurpés supprimés |
| user-service | `AuthServiceTest` | Inscription entreprise (email déjà pris, contenu du JWT), inscription employé (invitation expirée / annulée / valide) |
| user-service | `InvitationServiceTest` | Invitations employés : doublons, lien et expiration, droits entre entreprises |
| user-service | `UserAdminServiceTest`, `VoyageClientFallbackTest` | Désactivation d'un employé, appel Feign au voyage-service, repli si le service est arrêté |
| user-service | `UserIntegrationTest` | Connexion, contenu du jeton, 401 / 403 par rôle, API inter-services `/api/users/**` |
| voyage-service | `MatchingServiceTest` | Algorithme : distance Haversine, chevauchement des dates, périmètre, compatibilité des domaines, score |
| voyage-service | `VoyageServiceTest` | CRUD voyages : dates invalides, droits, employé récupéré via Feign, relance du matching, désactivation, filtres |
| voyage-service | `MatchServiceTest` | Invitations de voyage : seul le destinataire répond, coordonnées dévoilées uniquement après acceptation |
| voyage-service | `FallbacksTest` | Circuit breaker : tableau de bord disponible sans user-service / forum-service, 503 explicite, refus métier propagés |
| voyage-service | `VoyageIntegrationTest` | JWT accepté, validation, création avec matching, employé désactivé, cache rempli puis invalidé |
| forum-service | `ForumServiceTest` | Like / Dislike (ajout, annulation, bascule), auteur lu dans le jeton, suppression, tri, statistiques |
| ai-service | `AiServiceTest`, `ClaudeServiceTest`, `FallbacksTest` | Modération, découpage de l'explication, contenu des prompts, erreurs Feign, repli sans contexte, IA non configurée |

| Frontend (spec) | Ce qui est vérifié |
|---|---|
| `auth.service.spec` | Ouverture, restauration et expiration de la session, nouveau jeton après modification du profil, déconnexion |
| `auth.interceptor.spec` | Jeton ajouté aux seuls appels de l'API, déconnexion sur 401 (sauf échec de connexion), pas sur 403 |
| `guards.spec` | `authGuard` (redirection avec `returnUrl`), `guestGuard`, `roleGuard` par rôle |
| `api.service.spec` | Filtres des programmes, référentiel chargé une seule fois, routes d'invitation, Like / Dislike |
| `match-card.component.spec` | Partenaire et score affichés, coordonnées seulement après acceptation, bouton Accepter, analyse IA |
| `login.component.spec` | Validation du formulaire, comptes de démo, connexion et redirection, message d'erreur de l'API |
| `labels.spec`, `notify.service.spec` | Libellés français, dates sans décalage de fuseau, messages d'erreur de l'API |

## API REST (principaux endpoints, via la gateway)

| Méthode | URL | Rôle | Service |
|---|---|---|---|
| POST | `/api/auth/login`, `/api/auth/register-entreprise`, `/api/auth/register-employe` | public | user |
| GET | `/api/auth/invitation/{token}` | public | user |
| GET/PUT/DELETE | `/api/account/me`, PUT `/api/account/password`, GET/PUT `/api/account/entreprise` | connecté | user |
| GET/POST/DELETE | `/api/invitations`, POST `/api/invitations/{id}/renouveler` | ENTREPRISE | user |
| GET/PATCH | `/api/entreprise/employes`, `/api/entreprise/employes/{id}/statut` | ENTREPRISE | user |
| GET/PATCH | `/api/admin/users`, `/api/admin/users/{id}/statut`, `/api/admin/entreprises` | ADMIN | user |
| GET/POST/PUT/DELETE | `/api/voyages`, `/api/voyages/{id}` | EMPLOYE, ENTREPRISE | voyage |
| GET | `/api/voyages/programmes?destination=&objet=&domaine=&du=&au=` | connecté | voyage |
| GET/POST | `/api/voyages/{id}/matchs`, `/api/voyages/{id}/matching` | propriétaire | voyage |
| GET/POST | `/api/matchs/recues`, `/api/matchs/envoyees`, `/api/matchs/{id}/accepter`, `/api/matchs/{id}/refuser` | EMPLOYE | voyage |
| GET | `/api/stats/dashboard`, `/api/meta/villes` | connecté / public | voyage |
| GET/POST/PUT/DELETE | `/api/forum/posts`, `/api/forum/posts/{id}`, `/api/forum/posts/{id}/commentaires`, `/api/forum/posts/{id}/reaction` | connecté | forum |
| GET/POST | `/api/ai/**` | connecté | ai |

## Limites connues

- Un compte désactivé est refusé immédiatement par le user-service ; les autres services, qui ne lisent que le jeton,
  l'acceptent jusqu'à l'expiration du JWT (24 h). Ses voyages sortent toutefois tout de suite du matching.
- Les copies d'informations (nom de l'employé dans un voyage, auteur d'une publication) ne sont pas mises à jour
  rétroactivement après un changement de profil.

L'ancienne version monolithique est conservée dans `_archive/backend-monolithe` (peut être supprimée).
