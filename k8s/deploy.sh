#!/usr/bin/env sh
# Deploie Voyages & Affaires sur le cluster Kubernetes (kubeadm) pointe par kubectl.
#
#   ./k8s/deploy.sh <registry> <tag> [composant]
#     registry  : compte Docker Hub qui heberge les images (ex. nesrrine)
#     tag       : version des images (ex. numero du build Jenkins, ou latest)
#     composant : backend | frontend | monitoring | all (defaut : all)
#
# Lance depuis la racine du depot (c'est ce que fait Jenkins).
set -eu

REGISTRY="${1:?registry Docker Hub manquant (ex. nesrrine)}"
TAG="${2:?tag des images manquant (ex. 12 ou latest)}"
COMPOSANT="${3:-all}"
NS=voyages-affaires
DIR="$(cd "$(dirname "$0")" && pwd)"
RACINE="$(dirname "$DIR")"

# Applique un manifest en remplacant les marqueurs d'image
appliquer() {
  sed -e "s#__REGISTRY__#${REGISTRY}#g" -e "s#__TAG__#${TAG}#g" "$1" | kubectl apply -f -
}

# Cree / met a jour une ConfigMap a partir de fichiers (idempotent)
configmap() {
  nom="$1"; shift
  kubectl -n "$NS" create configmap "$nom" "$@" --dry-run=client -o yaml | kubectl apply -f -
}

echo ">> Namespace, configuration et base de donnees"
kubectl apply -f "$DIR/00-namespace.yaml"
kubectl apply -f "$DIR/01-config.yaml"
kubectl apply -f "$DIR/02-mysql.yaml"

if [ "$COMPOSANT" = "all" ] || [ "$COMPOSANT" = "backend" ]; then
  echo ">> Microservices (images ${REGISTRY}/va-*:${TAG})"
  for f in "$DIR"/0[3-8]-*.yaml; do appliquer "$f"; done
  kubectl apply -f "$DIR/20-autoscaling.yaml"
fi

if [ "$COMPOSANT" = "all" ] || [ "$COMPOSANT" = "frontend" ]; then
  echo ">> Frontend"
  appliquer "$DIR/09-frontend.yaml"
fi

if [ "$COMPOSANT" = "all" ] || [ "$COMPOSANT" = "monitoring" ] || [ "$COMPOSANT" = "backend" ]; then
  echo ">> Supervision (Prometheus + Grafana)"
  configmap prometheus-config \
    --from-file=prometheus.yml="$RACINE/monitoring/prometheus/prometheus-k8s.yml" \
    --from-file=alert-rules.yml="$RACINE/monitoring/prometheus/alert-rules.yml"
  configmap grafana-datasources --from-file="$RACINE/monitoring/grafana/provisioning/datasources/prometheus.yml"
  configmap grafana-dashboard-providers --from-file="$RACINE/monitoring/grafana/provisioning/dashboards/dashboards.yml"
  configmap grafana-dashboards --from-file="$RACINE/monitoring/grafana/dashboards/voyages-affaires.json"
  kubectl apply -f "$DIR/monitoring/prometheus.yaml"
  kubectl apply -f "$DIR/monitoring/grafana.yaml"
  # Prise en compte d'une configuration modifiee
  kubectl -n "$NS" rollout restart deployment/prometheus deployment/grafana
fi

echo ">> Attente des deploiements"
for d in $(kubectl -n "$NS" get deployments -o name); do
  kubectl -n "$NS" rollout status "$d" --timeout=300s
done

kubectl -n "$NS" get pods -o wide
echo "Application : http://<IP-d-un-noeud>:30080   Eureka : :30761   Prometheus : :30090   Grafana : :30030"
