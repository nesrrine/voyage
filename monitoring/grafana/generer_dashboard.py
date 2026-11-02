"""Genere le dashboard Grafana (voyages-affaires.json) : python generer_dashboard.py"""
import json
from pathlib import Path

DS = {"type": "prometheus", "uid": "prometheus"}
APP = 'application=~"$application"'
_id = 0


def panneau(titre, type_, x, y, w, h, cibles, unite=None, description=None, **extra):
    global _id
    _id += 1
    p = {
        "id": _id, "title": titre, "type": type_, "datasource": DS,
        "gridPos": {"x": x, "y": y, "w": w, "h": h},
        "targets": [{"datasource": DS, "refId": chr(65 + i), "expr": expr, "legendFormat": legende}
                    for i, (expr, legende) in enumerate(cibles)],
        "fieldConfig": {"defaults": {}, "overrides": []},
        "options": {},
    }
    if unite:
        p["fieldConfig"]["defaults"]["unit"] = unite
    if description:
        p["description"] = description
    p.update(extra)
    return p


def stat(titre, x, y, expr, unite=None, seuils=None, description=None):
    p = panneau(titre, "stat", x, y, 4, 4, [(expr, "")], unite, description)
    p["options"] = {"reduceOptions": {"calcs": ["lastNotNull"]}, "colorMode": "background", "graphMode": "area"}
    if seuils:
        p["fieldConfig"]["defaults"]["thresholds"] = {
            "mode": "absolute",
            "steps": [{"color": c, "value": v} for v, c in seuils],
        }
    return p


def courbe(titre, x, y, cibles, unite=None, w=12, h=8, description=None):
    p = panneau(titre, "timeseries", x, y, w, h, cibles, unite, description)
    p["fieldConfig"]["defaults"]["custom"] = {"lineWidth": 2, "fillOpacity": 10}
    p["options"] = {"legend": {"displayMode": "table", "placement": "bottom", "calcs": ["lastNotNull", "max"]}}
    return p


panels = [
    # ---- Ligne 1 : indicateurs cles ----
    stat("Services disponibles", 0, 0, f'sum(up{{{APP}}})',
         seuils=[(None, "red"), (5, "orange"), (6, "green")],
         description="Nombre de microservices dont Prometheus lit les metriques (6 attendus)."),
    stat("Requetes / s", 4, 0, f'sum(rate(http_server_requests_seconds_count{{{APP}}}[1m]))', "reqps"),
    stat("Erreurs serveur (5xx)", 8, 0,
         f'sum(rate(http_server_requests_seconds_count{{{APP},status=~"5.."}}[5m])) '
         f'/ clamp_min(sum(rate(http_server_requests_seconds_count{{{APP}}}[5m])), 1e-9)',
         "percentunit", seuils=[(None, "green"), (0.01, "orange"), (0.05, "red")]),
    stat("Latence p95", 12, 0,
         f'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{{{APP},uri!~"/api/ai.*"}}[5m])))',
         "s", seuils=[(None, "green"), (0.5, "orange"), (1, "red")]),
    stat("Invitations de voyage (matching)", 16, 0, 'sum(va_matching_invitations_total)',
         description="Metrique metier : invitations creees par l'algorithme de matching depuis le demarrage du voyage-service."),
    stat("Alertes actives", 20, 0, 'count(ALERTS{alertstate="firing"}) or vector(0)',
         seuils=[(None, "green"), (1, "red")]),

    # ---- Trafic et performances ----
    courbe("Requetes par seconde, par service", 0, 4,
           [(f'sum by (application) (rate(http_server_requests_seconds_count{{{APP}}}[1m]))', "{{application}}")], "reqps"),
    courbe("Latence p95 par service (hors IA)", 12, 4,
           [(f'histogram_quantile(0.95, sum by (le, application) (rate(http_server_requests_seconds_bucket{{{APP},uri!~"/api/ai.*"}}[5m])))',
             "{{application}}")], "s"),
    courbe("Reponses par code HTTP", 0, 12,
           [(f'sum by (status) (rate(http_server_requests_seconds_count{{{APP}}}[1m]))', "{{status}}")], "reqps",
           description="401 / 403 = refus de la securite (gateway ou microservice), 5xx = erreurs serveur."),
    courbe("Memoire JVM (tas) par service", 12, 12,
           [(f'sum by (application) (jvm_memory_used_bytes{{{APP},area="heap"}})', "{{application}}")], "bytes"),
    courbe("CPU par service", 0, 20,
           [(f'max by (application) (process_cpu_usage{{{APP}}})', "{{application}}")], "percentunit"),
    courbe("Threads JVM actifs", 12, 20,
           [(f'sum by (application) (jvm_threads_live_threads{{{APP}}})', "{{application}}")]),

    # ---- Resilience, cache et metier ----
    courbe("Circuit breakers ouverts (Resilience4j)", 0, 28,
           [('max by (application, name) (resilience4j_circuitbreaker_state{state="open"})', "{{application}} - {{name}}")],
           w=8, description="1 = circuit ouvert : le service appele est en panne et la reponse de repli est utilisee."),
    courbe("Taux de succes du cache (Caffeine)", 8, 28,
           [('sum by (cache) (rate(cache_gets_total{result="hit"}[5m])) / clamp_min(sum by (cache) (rate(cache_gets_total[5m])), 1e-9)',
             "{{cache}}")], "percentunit", w=8),
    courbe("Matching : invitations creees et score moyen", 16, 28,
           [('sum(increase(va_matching_invitations_total[5m]))', "invitations (5 min)"),
            ('sum(rate(va_matching_score_sum[15m])) / clamp_min(sum(rate(va_matching_score_count[15m])), 1e-9)', "score moyen")],
           w=8),
]

alertes = panneau("Alertes en cours", "table", 0, 36, 24, 6,
                  [('ALERTS{alertstate="firing"}', "")])
alertes["targets"][0]["format"] = "table"
alertes["targets"][0]["instant"] = True
alertes["transformations"] = [{"id": "organize", "options": {
    "excludeByName": {"Time": True, "Value": True, "__name__": True, "alertstate": True, "job": True},
    "renameByName": {"alertname": "Alerte", "application": "Service", "severity": "Gravite", "instance": "Instance"}}}]
panels.append(alertes)

dashboard = {
    "uid": "voyages-affaires",
    "title": "Voyages & Affaires - Microservices",
    "tags": ["voyages-affaires", "spring-boot", "microservices"],
    "timezone": "browser",
    "refresh": "10s",
    "time": {"from": "now-30m", "to": "now"},
    "schemaVersion": 39,
    "version": 1,
    "editable": True,
    "templating": {"list": [{
        "name": "application", "label": "Service", "type": "query", "datasource": DS,
        "query": {"query": "label_values(up, application)", "refId": "A"},
        "includeAll": True, "multi": True, "allValue": ".*",
        "current": {"selected": True, "text": "All", "value": "$__all"}, "refresh": 2,
    }]},
    "panels": panels,
}

sortie = Path(__file__).parent / "dashboards" / "voyages-affaires.json"
sortie.parent.mkdir(exist_ok=True)
sortie.write_text(json.dumps(dashboard, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print(f"{len(panels)} panneaux -> {sortie}")
