#!/bin/bash

set -e

ACCOUNT_ID=${AWS_ACCOUNT_ID}
REGION=${AWS_REGION:-us-east-1}
ECR_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/cep-tracker"

echo "Deploying CEP Tracker to AWS..."

# Login to ECR
echo "Logging in to ECR..."
aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URI}

# Push images
echo "Pushing images to ECR..."
docker push ${ECR_URI}:latest
docker push ${ECR_URI}:$(git rev-parse --short HEAD)

# Update ECS service
echo "Updating ECS service..."
aws ecs update-service \
    --cluster cep-tracker-cluster \
    --service cep-tracker-service \
    --force-new-deployment \
    --region ${REGION}

echo "Deployment completed successfully!"
