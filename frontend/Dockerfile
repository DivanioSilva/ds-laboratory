FROM node:24.20.0-alpine3.24 AS build
WORKDIR /workspace

COPY package.json ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:1.31-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
