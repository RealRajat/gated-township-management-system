# ==============================================================================
# Multi-stage Dockerfile for Township Management System Frontend (React + Vite)
# ==============================================================================

# Stage 1: Build stage
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies with lockfile caching
COPY package*.json ./
RUN npm ci

# Copy sources and compile production bundle
COPY . .
RUN npm run build

# Stage 2: Hardened Nginx static server
FROM nginx:alpine AS runner

# Remove default nginx config & copy customized config
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

