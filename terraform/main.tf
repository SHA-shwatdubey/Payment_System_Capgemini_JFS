terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Generate SSH Key Pair
resource "tls_private_key" "main" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "deployer" {
  key_name   = "${var.project_name}-deployer-key"
  public_key = tls_private_key.main.public_key_openssh

  tags = {
    Name        = "${var.project_name}-deployer-key"
    Environment = var.environment
    Project     = var.project_name
  }
}

# Security Group
resource "aws_security_group" "microservices" {
  name        = "${var.project_name}-microservices-sg"
  description = "Security group for microservices deployment"
  vpc_id      = data.aws_vpc.default.id

  tags = {
    Name        = "${var.project_name}-microservices-sg"
    Environment = var.environment
    Project     = var.project_name
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.ssh_cidr_blocks
    description = "SSH access"
  }

  ingress {
    from_port   = 8051
    to_port     = 8051
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Frontend"
  }

  ingress {
    from_port   = 8060
    to_port     = 8062
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Config Server, Eureka, API Gateway"
  }

  ingress {
    from_port   = 8063
    to_port     = 8070
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Microservices"
  }

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "PostgreSQL"
  }

  ingress {
    from_port   = 15672
    to_port     = 15672
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "RabbitMQ Management"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }
}

# Data source for default VPC
data "aws_vpc" "default" {
  default = true
}

# Get latest Ubuntu 22.04 AMI
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
}

# EC2 Instance
resource "aws_instance" "microservices" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  key_name               = aws_key_pair.deployer.key_name
  vpc_security_group_ids = [aws_security_group.microservices.id]

  associate_public_ip_address = true
  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_size
    delete_on_termination = true
  }

  user_data = base64encode(templatefile("${path.module}/user_data.sh", {
    docker_compose_content = file("${path.module}/../docker-compose.yml")
  }))

  monitoring = true

  tags = {
    Name        = "${var.project_name}-microservices-server"
    Environment = var.environment
    Project     = var.project_name
  }

  depends_on = [aws_security_group.microservices]
}

# Store private key locally
resource "local_file" "private_key" {
  content  = tls_private_key.main.private_key_pem
  filename = "${path.module}/${var.project_name}-deployer-key.pem"

  file_permission = "0600"

  depends_on = [tls_private_key.main]
}

