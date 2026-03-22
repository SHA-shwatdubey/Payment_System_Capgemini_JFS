$ErrorActionPreference = "Continue"

$project = "payment-system-capgemini"
$region = "ap-south-1"

Write-Host "== Terraform init ==" -ForegroundColor Cyan
terraform init -reconfigure

Write-Host "== Import IAM Roles ==" -ForegroundColor Cyan
terraform import aws_iam_role.eks_cluster "$project-eks-cluster-role"
terraform import aws_iam_role.eks_node_group "$project-eks-node-group-role"

Write-Host "== Import CloudWatch Log Group ==" -ForegroundColor Cyan
terraform import aws_cloudwatch_log_group.eks "/aws/eks/$project-cluster/cluster"

Write-Host "== Import ECR Repositories ==" -ForegroundColor Cyan
$repos = @(
  "config-server",
  "eureka-server",
  "api-gateway",
  "auth-service",
  "user-kyc-service",
  "wallet-service",
  "notification-service",
  "rewards-service",
  "transaction-service",
  "integration-service",
  "admin-service"
)

foreach ($r in $repos) {
  $tfAddress = "aws_ecr_repository.services[\"$r\"]"
  $repoName = "$project-$r"
  Write-Host "Importing $repoName ..." -ForegroundColor Yellow
  terraform import "$tfAddress" "$repoName"
}

Write-Host "== Refresh plan after imports ==" -ForegroundColor Cyan
terraform plan

Write-Host "== Apply ==" -ForegroundColor Cyan
terraform apply -auto-approve
