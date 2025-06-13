#!/bin/bash

set -e

echo "Building CEP Tracker Application..."

# Build the application
echo "Compiling Java application..."
./mvnw clean package -DskipTests

# Build Docker image
echo "Building Docker image..."
docker build -t cep-tracker:latest .

# Tag for AWS ECR (replace with your account and region)
ACCOUNT_ID=${AWS_ACCOUNT_ID:-123456789012}
REGION=${AWS_REGION:-us-east-1}
ECR_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/cep-tracker"

docker tag cep-tracker:latest ${ECR_URI}:latest
docker tag cep-tracker:latest ${ECR_URI}:$(git rev-parse --short HEAD)

echo "Build completed successfully!"
echo " Tagged images:"
echo "  - cep-tracker:latest"
echo "  - ${ECR_URI}:latest"
echo "  - ${ECR_URI}:$(git rev-parse --short HEAD)"
