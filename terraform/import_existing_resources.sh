#!/usr/bin/env bash
set -uo pipefail

PROJECT="${PROJECT:-payment-system-capgemini}"
REGION="${AWS_REGION:-ap-south-1}"

# Keep imports idempotent for stateless CI runners.
import_if_exists() {
  local tf_addr="$1"
  local cloud_exists_cmd="$2"
  local resource_id="$3"

  if eval "$cloud_exists_cmd" >/dev/null 2>&1; then
    terraform state show "$tf_addr" >/dev/null 2>&1 || terraform import "$tf_addr" "$resource_id" || true
  fi
}

echo "== Importing existing resources for project: ${PROJECT} (region: ${REGION}) =="

# Core network resources (prevents VPC re-creation on stateless runners)
vpc_id="$(aws ec2 describe-vpcs --region "${REGION}" --filters "Name=tag:Name,Values=${PROJECT}-vpc" --query 'Vpcs[0].VpcId' --output text 2>/dev/null || true)"
if [[ -n "${vpc_id}" && "${vpc_id}" != "None" ]]; then
  terraform state show aws_vpc.main >/dev/null 2>&1 || terraform import aws_vpc.main "${vpc_id}" || true

  public_subnet_1="$(aws ec2 describe-subnets --region "${REGION}" --filters "Name=vpc-id,Values=${vpc_id}" "Name=tag:Name,Values=${PROJECT}-public-subnet-1" --query 'Subnets[0].SubnetId' --output text 2>/dev/null || true)"
  public_subnet_2="$(aws ec2 describe-subnets --region "${REGION}" --filters "Name=vpc-id,Values=${vpc_id}" "Name=tag:Name,Values=${PROJECT}-public-subnet-2" --query 'Subnets[0].SubnetId' --output text 2>/dev/null || true)"
  private_subnet_1="$(aws ec2 describe-subnets --region "${REGION}" --filters "Name=vpc-id,Values=${vpc_id}" "Name=tag:Name,Values=${PROJECT}-private-subnet-1" --query 'Subnets[0].SubnetId' --output text 2>/dev/null || true)"
  private_subnet_2="$(aws ec2 describe-subnets --region "${REGION}" --filters "Name=vpc-id,Values=${vpc_id}" "Name=tag:Name,Values=${PROJECT}-private-subnet-2" --query 'Subnets[0].SubnetId' --output text 2>/dev/null || true)"

  [[ -n "${public_subnet_1}" && "${public_subnet_1}" != "None" ]] && (terraform state show 'aws_subnet.public[0]' >/dev/null 2>&1 || terraform import 'aws_subnet.public[0]' "${public_subnet_1}" || true)
  [[ -n "${public_subnet_2}" && "${public_subnet_2}" != "None" ]] && (terraform state show 'aws_subnet.public[1]' >/dev/null 2>&1 || terraform import 'aws_subnet.public[1]' "${public_subnet_2}" || true)
  [[ -n "${private_subnet_1}" && "${private_subnet_1}" != "None" ]] && (terraform state show 'aws_subnet.private[0]' >/dev/null 2>&1 || terraform import 'aws_subnet.private[0]' "${private_subnet_1}" || true)
  [[ -n "${private_subnet_2}" && "${private_subnet_2}" != "None" ]] && (terraform state show 'aws_subnet.private[1]' >/dev/null 2>&1 || terraform import 'aws_subnet.private[1]' "${private_subnet_2}" || true)

  igw_id="$(aws ec2 describe-internet-gateways --region "${REGION}" --filters "Name=attachment.vpc-id,Values=${vpc_id}" "Name=tag:Name,Values=${PROJECT}-igw" --query 'InternetGateways[0].InternetGatewayId' --output text 2>/dev/null || true)"
  [[ -n "${igw_id}" && "${igw_id}" != "None" ]] && (terraform state show aws_internet_gateway.main >/dev/null 2>&1 || terraform import aws_internet_gateway.main "${igw_id}" || true)

  # Import NAT first, then import the exact EIP allocation attached to that NAT.
  nat_id="$(aws ec2 describe-nat-gateways --region "${REGION}" --filter "Name=vpc-id,Values=${vpc_id}" "Name=state,Values=available,pending" --query 'NatGateways[0].NatGatewayId' --output text 2>/dev/null || true)"
  if [[ -n "${nat_id}" && "${nat_id}" != "None" ]]; then
    terraform state show 'aws_nat_gateway.main[0]' >/dev/null 2>&1 || terraform import 'aws_nat_gateway.main[0]' "${nat_id}" || true

    eip_alloc_id="$(aws ec2 describe-nat-gateways --region "${REGION}" --nat-gateway-ids "${nat_id}" --query 'NatGateways[0].NatGatewayAddresses[0].AllocationId' --output text 2>/dev/null || true)"
    [[ -n "${eip_alloc_id}" && "${eip_alloc_id}" != "None" ]] && (terraform state show 'aws_eip.nat[0]' >/dev/null 2>&1 || terraform import 'aws_eip.nat[0]' "${eip_alloc_id}" || true)
  fi

  public_rt_id="$(aws ec2 describe-route-tables --region "${REGION}" --filters "Name=vpc-id,Values=${vpc_id}" "Name=tag:Name,Values=${PROJECT}-public-rt" --query 'RouteTables[0].RouteTableId' --output text 2>/dev/null || true)"
  private_rt_id="$(aws ec2 describe-route-tables --region "${REGION}" --filters "Name=vpc-id,Values=${vpc_id}" "Name=tag:Name,Values=${PROJECT}-private-rt" --query 'RouteTables[0].RouteTableId' --output text 2>/dev/null || true)"
  [[ -n "${public_rt_id}" && "${public_rt_id}" != "None" ]] && (terraform state show aws_route_table.public >/dev/null 2>&1 || terraform import aws_route_table.public "${public_rt_id}" || true)
  [[ -n "${private_rt_id}" && "${private_rt_id}" != "None" ]] && (terraform state show aws_route_table.private >/dev/null 2>&1 || terraform import aws_route_table.private "${private_rt_id}" || true)

  if [[ -n "${public_rt_id}" && "${public_rt_id}" != "None" ]]; then
    pub_assoc_0="$(aws ec2 describe-route-tables --region "${REGION}" --route-table-ids "${public_rt_id}" --query "RouteTables[0].Associations[?SubnetId=='${public_subnet_1}'].RouteTableAssociationId | [0]" --output text 2>/dev/null || true)"
    pub_assoc_1="$(aws ec2 describe-route-tables --region "${REGION}" --route-table-ids "${public_rt_id}" --query "RouteTables[0].Associations[?SubnetId=='${public_subnet_2}'].RouteTableAssociationId | [0]" --output text 2>/dev/null || true)"
    [[ -n "${pub_assoc_0}" && "${pub_assoc_0}" != "None" ]] && (terraform state show 'aws_route_table_association.public[0]' >/dev/null 2>&1 || terraform import 'aws_route_table_association.public[0]' "${pub_assoc_0}" || true)
    [[ -n "${pub_assoc_1}" && "${pub_assoc_1}" != "None" ]] && (terraform state show 'aws_route_table_association.public[1]' >/dev/null 2>&1 || terraform import 'aws_route_table_association.public[1]' "${pub_assoc_1}" || true)
  fi

  if [[ -n "${private_rt_id}" && "${private_rt_id}" != "None" ]]; then
    pvt_assoc_0="$(aws ec2 describe-route-tables --region "${REGION}" --route-table-ids "${private_rt_id}" --query "RouteTables[0].Associations[?SubnetId=='${private_subnet_1}'].RouteTableAssociationId | [0]" --output text 2>/dev/null || true)"
    pvt_assoc_1="$(aws ec2 describe-route-tables --region "${REGION}" --route-table-ids "${private_rt_id}" --query "RouteTables[0].Associations[?SubnetId=='${private_subnet_2}'].RouteTableAssociationId | [0]" --output text 2>/dev/null || true)"
    [[ -n "${pvt_assoc_0}" && "${pvt_assoc_0}" != "None" ]] && (terraform state show 'aws_route_table_association.private[0]' >/dev/null 2>&1 || terraform import 'aws_route_table_association.private[0]' "${pvt_assoc_0}" || true)
    [[ -n "${pvt_assoc_1}" && "${pvt_assoc_1}" != "None" ]] && (terraform state show 'aws_route_table_association.private[1]' >/dev/null 2>&1 || terraform import 'aws_route_table_association.private[1]' "${pvt_assoc_1}" || true)
  fi

  alb_sg_id="$(aws ec2 describe-security-groups --region "${REGION}" --filters "Name=vpc-id,Values=${vpc_id}" "Name=group-name,Values=${PROJECT}-alb-sg" --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null || true)"
  eks_sg_id="$(aws ec2 describe-security-groups --region "${REGION}" --filters "Name=vpc-id,Values=${vpc_id}" "Name=group-name,Values=${PROJECT}-eks-nodes-sg" --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null || true)"
  rds_sg_id="$(aws ec2 describe-security-groups --region "${REGION}" --filters "Name=vpc-id,Values=${vpc_id}" "Name=group-name,Values=${PROJECT}-rds-sg" --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null || true)"
  [[ -n "${alb_sg_id}" && "${alb_sg_id}" != "None" ]] && (terraform state show aws_security_group.alb >/dev/null 2>&1 || terraform import aws_security_group.alb "${alb_sg_id}" || true)
  [[ -n "${eks_sg_id}" && "${eks_sg_id}" != "None" ]] && (terraform state show aws_security_group.eks_nodes >/dev/null 2>&1 || terraform import aws_security_group.eks_nodes "${eks_sg_id}" || true)
  [[ -n "${rds_sg_id}" && "${rds_sg_id}" != "None" ]] && (terraform state show aws_security_group.rds >/dev/null 2>&1 || terraform import aws_security_group.rds "${rds_sg_id}" || true)
fi

import_if_exists "aws_iam_role.eks_cluster" \
  "aws iam get-role --role-name ${PROJECT}-eks-cluster-role" \
  "${PROJECT}-eks-cluster-role"

import_if_exists "aws_iam_role.eks_node_group" \
  "aws iam get-role --role-name ${PROJECT}-eks-node-group-role" \
  "${PROJECT}-eks-node-group-role"

import_if_exists "aws_cloudwatch_log_group.eks" \
  "aws logs describe-log-groups --region ${REGION} --log-group-name-prefix /aws/eks/${PROJECT}-cluster/cluster --query 'logGroups[0].logGroupName' --output text | grep -q '/aws/eks/${PROJECT}-cluster/cluster'" \
  "/aws/eks/${PROJECT}-cluster/cluster"

import_if_exists "aws_eks_cluster.main" \
  "aws eks describe-cluster --name ${PROJECT}-cluster --region ${REGION}" \
  "${PROJECT}-cluster"

# Ensure cluster import happened if cluster exists in AWS.
if aws eks describe-cluster --name "${PROJECT}-cluster" --region "${REGION}" >/dev/null 2>&1; then
  terraform state show aws_eks_cluster.main >/dev/null 2>&1 || terraform import aws_eks_cluster.main "${PROJECT}-cluster" || true
fi

services=(
  config-server
  eureka-server
  api-gateway
  auth-service
  user-kyc-service
  wallet-service
  notification-service
  rewards-service
  transaction-service
  integration-service
  admin-service
)

for svc in "${services[@]}"; do
  repo="${PROJECT}-${svc}"
  tf_addr="aws_ecr_repository.services[\"${svc}\"]"
  import_if_exists "$tf_addr" \
    "aws ecr describe-repositories --region ${REGION} --repository-names ${repo}" \
    "$repo"
done

alb_arn="$(aws elbv2 describe-load-balancers --region "${REGION}" --names "${PROJECT}-alb" --query 'LoadBalancers[0].LoadBalancerArn' --output text 2>/dev/null || true)"
if [[ -n "${alb_arn}" && "${alb_arn}" != "None" ]]; then
  terraform state show aws_lb.main >/dev/null 2>&1 || terraform import aws_lb.main "${alb_arn}" || true

  listener_arn="$(aws elbv2 describe-listeners --region "${REGION}" --load-balancer-arn "${alb_arn}" --query 'Listeners[0].ListenerArn' --output text 2>/dev/null || true)"
  if [[ -n "${listener_arn}" && "${listener_arn}" != "None" ]]; then
    terraform state show aws_lb_listener.main >/dev/null 2>&1 || terraform import aws_lb_listener.main "${listener_arn}" || true
  fi
fi

tg_arn="$(aws elbv2 describe-target-groups --region "${REGION}" --names "${PROJECT}-tg" --query 'TargetGroups[0].TargetGroupArn' --output text 2>/dev/null || true)"
if [[ -n "${tg_arn}" && "${tg_arn}" != "None" ]]; then
  terraform state show aws_lb_target_group.main >/dev/null 2>&1 || terraform import aws_lb_target_group.main "${tg_arn}" || true
fi

import_if_exists "aws_db_subnet_group.main" \
  "aws rds describe-db-subnet-groups --region ${REGION} --db-subnet-group-name ${PROJECT}-db-subnet-group" \
  "${PROJECT}-db-subnet-group"

import_if_exists "aws_db_instance.main" \
  "aws rds describe-db-instances --region ${REGION} --db-instance-identifier ${PROJECT}-db" \
  "${PROJECT}-db"

echo "== Import step complete =="
