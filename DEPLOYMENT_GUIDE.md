# Microservices Deployment Guide
## Prerequisites
- AWS Account with appropriate permissions
- Terraform >= 1.0
- AWS CLI configured
- GitHub repository with SSH keys set up
## Setup Instructions
### 1. Add GitHub Secrets
Go to your GitHub repository → Settings → Secrets and add:
\\\
SSH_PRIVATE_KEY: (Content from terraform/payment-system-deployer-key.pem after terraform apply)
EC2_PUBLIC_IP: (EC2 instance public IP from terraform output)
\\\
### 2. Deploy Infrastructure
\\\ash
cd terraform
# Initialize Terraform
terraform init
# Plan deployment
terraform plan -out=tfplan
# Apply configuration
terraform apply tfplan
\\\
### 3. Retrieve Outputs
After terraform apply completes:
\\\ash
terraform output -raw ec2_public_ip
terraform output -raw private_key_path
terraform output deployment_info
\\\
### 4. Update GitHub Secrets
Copy the output values and add them to GitHub Secrets.
### 5. Deploy Application
Push to main branch to trigger deployment:
\\\ash
git add .
git commit -m "Deploy microservices"
git push origin main
\\\
## Manual Deployment (if needed)
SSH into EC2 and run:
\\\ash
ssh -i <path-to-key> ubuntu@<ec2-public-ip>
# Once connected
cd /home/ubuntu/app
# Restart services
docker-compose down
docker-compose pull
docker-compose up -d
# Check status
docker-compose ps
# View logs
docker-compose logs -f
\\\
## Access Services
- **Frontend**: http://<EC2_IP>:8051
- **Eureka Dashboard**: http://<EC2_IP>:8061
- **Swagger UI**: http://<EC2_IP>:8062/swagger-ui.html
- **Auth Service**: http://<EC2_IP>:8063
- **User KYC Service**: http://<EC2_IP>:8064
- **Wallet Service**: http://<EC2_IP>:8065
- **Rewards Service**: http://<EC2_IP>:8066
- **Admin Service**: http://<EC2_IP>:8067
- **Transaction Service**: http://<EC2_IP>:8068
- **Notification Service**: http://<EC2_IP>:8069
- **Integration Service**: http://<EC2_IP>:8070
- **Config Server**: http://<EC2_IP>:8060
- **RabbitMQ Management**: http://<EC2_IP>:15672
## Database & Message Queue
- **PostgreSQL**: <EC2_IP>:5432
- **RabbitMQ**: <EC2_IP>:5672
## Troubleshooting
### Check service health
\\\ash
ssh -i <key> ubuntu@<ec2-ip>
cd /home/ubuntu/app
docker-compose ps
docker-compose logs <service-name>
\\\
### Restart specific service
\\\ash
docker-compose restart <service-name>
\\\
### View all logs
\\\ash
docker-compose logs -f
\\\
### Pull latest images
\\\ash
docker-compose pull
docker-compose up -d
\\\
## Cleanup
To destroy all resources:
\\\ash
cd terraform
terraform destroy
\\\
This will:
- Terminate EC2 instance
- Delete security group
- Delete SSH key pair
- Delete volumes
