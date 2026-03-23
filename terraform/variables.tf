variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "ap-south-1"
}

variable "project_name" {
  description = "Project Name"
  type        = string
  default     = "payment-system-capgemini"
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "nat_gateway_count" {
  description = "Number of NAT gateways/EIPs to create"
  type        = number
  default     = 1
}

# EKS Variables
variable "eks_kubernetes_version" {
  description = "EKS Kubernetes version for cluster and node group"
  type        = string
  default     = "1.30"
}

variable "eks_node_ami_type" {
  description = "AMI type for EKS managed node group"
  type        = string
  default     = "AL2023_x86_64_STANDARD"
}

variable "node_instance_type" {
  description = "EC2 Instance Type for EKS Nodes"
  type        = string
  default     = "t3.micro"
}

variable "node_desired_size" {
  description = "Desired number of worker nodes"
  type        = number
  default     = 2
}

variable "node_min_size" {
  description = "Minimum number of worker nodes"
  type        = number
  default     = 1
}

variable "node_max_size" {
  description = "Maximum number of worker nodes"
  type        = number
  default     = 4
}

# RDS Variables
variable "db_instance_class" {
  description = "RDS Instance Class (free-tier: db.t2.micro or db.t3.micro)"
  type        = string
  default     = "db.t2.micro"
}

variable "db_allocated_storage" {
  description = "Allocated storage in GB"
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Database Name"
  type        = string
  default     = "payment_db"
  sensitive   = true
}

variable "db_username" {
  description = "Database Username"
  type        = string
  default     = "postgres"
  sensitive   = true
}

variable "db_password" {
  description = "Database Password"
  type        = string
  default     = ""
  sensitive   = true

  validation {
    condition     = var.db_password == "" || length(trimspace(var.db_password)) >= 8
    error_message = "db_password must be at least 8 characters, or left empty to auto-generate a strong password."
  }
}

variable "db_multi_az" {
  description = "Multi-AZ deployment"
  type        = bool
  default     = false
}

variable "db_backup_retention_period" {
  description = "RDS backup retention in days (set low for free-tier constrained accounts)"
  type        = number
  default     = 1
}

variable "skip_final_snapshot" {
  description = "Skip final snapshot on destroy"
  type        = bool
  default     = true
}
