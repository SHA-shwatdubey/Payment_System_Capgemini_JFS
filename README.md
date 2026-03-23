# Production Deployment Infrastructure
This directory contains all infrastructure-as-code and deployment configurations for the Payment & Reward System microservices.
## Files Overview
### Terraform Configuration
- **terraform/main.tf** - AWS infrastructure setup (EC2, Security Group, SSH Key)
- **terraform/variables.tf** - Terraform variables and defaults
- **terraform/outputs.tf** - Terraform outputs for easy access
- **terraform/user_data.sh** - EC2 initialization script (Docker, Docker Compose)
### Docker Deployment
- **docker-compose.yml** - Complete microservices stack configuration
  - 2 infrastructure services (PostgreSQL, RabbitMQ)
  - 2 core services (Eureka, Config Server)
  - 1 API Gateway
  - 8 business logic microservices
  - 1 Frontend
### CI/CD Pipeline
- **.github/workflows/deploy.yml** - Automated deployment on push to main
## Quick Start
### 1. Initialize AWS Infrastructure
\\\ash
cd terraform
terraform init
terraform plan
terraform apply
\\\
### 2. Retrieve Deployment Information
\\\ash
terraform output deployment_info
terraform output ec2_public_ip
\\\
### 3. Configure GitHub Secrets
Copy SSH private key path output and EC2 IP to GitHub repository secrets:
- SSH_PRIVATE_KEY
- EC2_PUBLIC_IP
### 4. Deploy via GitHub Actions
Push to main branch:
\\\ash
git push origin main
\\\
## Architecture
### Services & Ports
| Service | Port | Purpose |
|---------|------|---------|
| Frontend | 8051 | Angular/Web UI |
| Config Server | 8060 | Spring Cloud Config |
| Eureka | 8061 | Service Registry |
| API Gateway | 8062 | Request Router |
| Auth Service | 8063 | Authentication |
| User KYC Service | 8064 | KYC Verification |
| Wallet Service | 8065 | Wallet Management |
| Rewards Service | 8066 | Rewards Engine |
| Admin Service | 8067 | Admin Dashboard |
| Transaction Service | 8068 | Transaction Processing |
| Notification Service | 8069 | Notifications |
| Integration Service | 8070 | Third-party Integration |
| PostgreSQL | 5432 | Database |
| RabbitMQ | 5672 | Message Queue |
| RabbitMQ Mgmt | 15672 | RabbitMQ Dashboard |
### Infrastructure
- **AWS Region**: ap-south-1 (Mumbai)
- **Instance Type**: t3.medium
- **OS**: Ubuntu 22.04 LTS
- **Storage**: 50GB GP3 EBS
- **Networking**: VPC with custom security group
## Deployment Flow
\\\
GitHub Push (main)
    ↓
GitHub Actions Trigger
    ↓
SSH to EC2
    ↓
docker-compose down
    ↓
docker-compose pull (latest images)
    ↓
docker-compose up -d
    ↓
Health checks
\\\
## Health Checks
All services include health checks:
- Eureka: /eureka/status
- Config Server: /actuator/health
- API Gateway: /actuator/health
- Microservices: /actuator/health
## Monitoring
### View Service Status
\\\ash
ssh -i <key> ubuntu@<ip>
cd /home/ubuntu/app
docker-compose ps
\\\
### View Logs
\\\ash
docker-compose logs -f <service-name>
\\\
### Access Dashboards
- Eureka: http://<EC2_IP>:8061
- RabbitMQ: http://<EC2_IP>:15672 (admin/admin123)
- Swagger: http://<EC2_IP>:8062/swagger-ui.html
## Security Features
- SSH key-based authentication
- Custom security group with minimal port exposure
- Environment variables for sensitive data
- Restart policy: always (auto-recovery)
- Health checks with automatic restart on failure
## Troubleshooting
### Services not starting
\\\ash
docker-compose logs
docker-compose restart <service>
\\\
### Connection refused
Check security group in AWS Console - verify all required ports are open.
### Out of disk space
\\\ash
docker system prune
docker volume prune
\\\
## Cleanup
Remove all resources:
\\\ash
cd terraform
terraform destroy
\\\
## Support
Refer to DEPLOYMENT_GUIDE.md for detailed step-by-step instructions.
