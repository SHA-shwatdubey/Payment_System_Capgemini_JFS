# AWS Load Balancer Cleanup Script
# This script manually removes the stuck target group, listener, and ALB from AWS

$region = "ap-south-1"
$projectName = "payment-system-capgemini"

Write-Host "=== AWS Load Balancer Cleanup ===" -ForegroundColor Cyan

# Find and delete listeners
Write-Host "Finding listeners..." -ForegroundColor Yellow
$listeners = aws elbv2 describe-listeners --region $region --output json | ConvertFrom-Json

foreach ($listener in $listeners.Listeners) {
  Write-Host "Deleting listener: $($listener.ListenerArn)" -ForegroundColor Yellow
  aws elbv2 delete-listener --listener-arn $listener.ListenerArn --region $region 2>&1 | Out-Null
  Start-Sleep -Seconds 3
}

# Find and delete target groups
Write-Host "Finding target groups..." -ForegroundColor Yellow
$tgs = aws elbv2 describe-target-groups --region $region --output json | ConvertFrom-Json

foreach ($tg in $tgs.TargetGroups) {
  if ($tg.TargetGroupName -like "*capgemini*") {
    Write-Host "Deleting target group: $($tg.TargetGroupName)" -ForegroundColor Yellow
    aws elbv2 delete-target-group --target-group-arn $tg.TargetGroupArn --region $region 2>&1 | Out-Null
    Start-Sleep -Seconds 3
  }
}

# Find and delete load balancers
Write-Host "Finding load balancers..." -ForegroundColor Yellow
$albs = aws elbv2 describe-load-balancers --region $region --output json | ConvertFrom-Json

foreach ($alb in $albs.LoadBalancers) {
  if ($alb.LoadBalancerName -like "*capgemini*") {
    Write-Host "Deleting ALB: $($alb.LoadBalancerName)" -ForegroundColor Yellow
    aws elbv2 delete-load-balancer --load-balancer-arn $alb.LoadBalancerArn --region $region 2>&1 | Out-Null
    Start-Sleep -Seconds 3
  }
}

Write-Host "Cleanup complete!" -ForegroundColor Green


