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
