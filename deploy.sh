#!/bin/bash

# Deploy script for PaperInsight

echo "Build and starting PaperInsight services..."

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "docker-compose could not be found. Please install it first."
    exit 1
fi

# Pull latest images (if using remote images)
# docker-compose pull

# Build and start containers in detached mode
docker-compose up -d --build

echo "Deployment started!"
echo "Frontend: http://localhost:80"
echo "Backend API: http://localhost:8080"
echo "RabbitMQ Dashboard: http://localhost:15672"
