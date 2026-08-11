# Stage 1: build
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: serve. Nginx proxies /api to the backend and falls back to index.html for
# client-side routes — no CORS and no environment-specific API URL needed in production,
# since the browser only ever talks to the origin nginx is serving.
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
