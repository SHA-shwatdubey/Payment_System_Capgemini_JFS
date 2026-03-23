output "ec2_instance_id" {
  description = "EC2 Instance ID"
  value       = aws_instance.microservices.id
}

output "ec2_public_ip" {
  description = "EC2 Public IP Address"
  value       = aws_instance.microservices.public_ip
}

output "ec2_private_ip" {
  description = "EC2 Private IP Address"
  value       = aws_instance.microservices.private_ip
}

output "security_group_id" {
  description = "Security Group ID"
  value       = aws_security_group.microservices.id
}

output "security_group_name" {
  description = "Security Group Name"
  value       = aws_security_group.microservices.name
}

output "ssh_key_name" {
  description = "SSH Key Pair Name"
  value       = aws_key_pair.deployer.key_name
}

output "private_key_path" {
  description = "Path to private key file"
  value       = local_file.private_key.filename
  sensitive   = true
}

output "ec2_public_dns" {
  description = "EC2 Public DNS"
  value       = aws_instance.microservices.public_dns
}

output "deployment_info" {
  description = "Deployment access information"
  value = {
    ssh_command = "ssh -i ${local_file.private_key.filename} ubuntu@${aws_instance.microservices.public_ip}"
    ec2_ip      = aws_instance.microservices.public_ip
    frontend_url = "http://${aws_instance.microservices.public_ip}:8051"
    eureka_url   = "http://${aws_instance.microservices.public_ip}:8061"
    swagger_url  = "http://${aws_instance.microservices.public_ip}:8062/swagger-ui.html"
  }
}

