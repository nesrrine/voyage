# ---------- Etape 1 : compilation de l'application Angular ----------
FROM node:20-alpine AS build
WORKDIR /app
# Les dependances sont installees dans une couche a part (mise en cache tant que package*.json ne change pas)
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

# ---------- Etape 2 : serveur web nginx (image finale legere) ----------
FROM nginx:1.27-alpine
LABEL org.opencontainers.image.title="voyages-affaires/frontend" \
      org.opencontainers.image.source="https://github.com/nesrrine/voyage"
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
