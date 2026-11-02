"""Genere les manifests Kubernetes des microservices (python generer_manifests.py).

Les images utilisent les marqueurs __REGISTRY__ et __TAG__, remplaces au deploiement par deploy.sh
(ex. : nesrrine / numero du build Jenkins).
"""
from pathlib import Path

SERVICES = [
    # nom, port, base MySQL, expose en NodePort
    ("discovery-server", 8761, None, 30761),
    ("user-service", 8081, "va_users", None),
    ("voyage-service", 8082, "va_voyages", None),
    ("forum-service", 8083, "va_forum", None),
    ("ai-service", 8084, None, None),
    ("api-gateway", 8080, None, None),
]

GABARIT = """# Genere par generer_manifests.py - ne pas modifier a la main
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {nom}
  namespace: voyages-affaires
  labels:
    app: {nom}
    app.kubernetes.io/part-of: voyages-affaires
spec:
  replicas: 1
  revisionHistoryLimit: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0      # mise a jour sans interruption
  selector:
    matchLabels:
      app: {nom}
  template:
    metadata:
      labels:
        app: {nom}
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: /actuator/prometheus
        prometheus.io/port: "{port}"
    spec:
{init}      containers:
        - name: {nom}
          image: __REGISTRY__/va-{nom}:__TAG__
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: {port}
          envFrom:
            - configMapRef:
                name: va-config
          env:
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef: {{ name: va-secrets, key: JWT_SECRET }}
{env}          resources:
            requests: {{ cpu: 100m, memory: 384Mi }}
            limits: {{ cpu: "1", memory: 768Mi }}
          # Demarrage de la JVM : jusqu'a 3 minutes avant que les autres sondes prennent le relais
          startupProbe:
            httpGet: {{ path: /actuator/health/liveness, port: http }}
            periodSeconds: 5
            failureThreshold: 36
          livenessProbe:
            httpGet: {{ path: /actuator/health/liveness, port: http }}
            periodSeconds: 15
            failureThreshold: 3
          readinessProbe:
            httpGet: {{ path: /actuator/health/readiness, port: http }}
            periodSeconds: 10
            failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: {nom}
  namespace: voyages-affaires
  labels:
    app: {nom}
spec:
  type: {type_service}
  selector:
    app: {nom}
  ports:
    - name: http
      port: {port}
      targetPort: http
{node_port}"""

ATTENTE_MYSQL = """      # Attend que MySQL accepte les connexions avant de demarrer Spring Boot
      initContainers:
        - name: attente-mysql
          image: busybox:1.36
          command: ["sh", "-c", "until nc -z mysql 3306; do echo 'attente de mysql'; sleep 3; done"]
"""

for i, (nom, port, base, node_port) in enumerate(SERVICES, start=3):
    env = ""
    if base:
        env += f"""            - name: SPRING_DATASOURCE_URL
              value: jdbc:mysql://mysql:3306/{base}?createDatabaseIfNotExist=true&serverTimezone=UTC
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef: {{ name: va-secrets, key: MYSQL_ROOT_PASSWORD }}
"""
    if nom == "ai-service":
        env += """            - name: ANTHROPIC_API_KEY
              valueFrom:
                secretKeyRef: { name: va-secrets, key: ANTHROPIC_API_KEY }
"""
    contenu = GABARIT.format(
        nom=nom, port=port, env=env,
        init=ATTENTE_MYSQL if base else "",
        type_service="NodePort" if node_port else "ClusterIP",
        node_port=f"      nodePort: {node_port}\n" if node_port else "",
    )
    Path(__file__).with_name(f"{i:02d}-{nom}.yaml").write_text(contenu, encoding="utf-8")
    print(f"{i:02d}-{nom}.yaml")
