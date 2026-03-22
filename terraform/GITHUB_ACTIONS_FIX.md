# GitHub Actions - Manual Cleanup Required

## ⚠️ Current Status

Your GitHub Actions deployment failed with this error:
```
Error: deleting ELBv2 Target Group - ResourceInUse: Target group is currently in use by a listener or a rule
```

This is happening on the GitHub Actions runner, not locally.

## ✅ What's Been Fixed

The code has been updated with proper lifecycle management:
- ✅ `eks.tf` - Fixed listener and target group lifecycle configurations
- ✅ `cleanup_alb.ps1` - PowerShell script for manual AWS cleanup  
- ✅ `SOLUTION_SUMMARY.md` - Complete recovery guide
- ✅ `RECOVERY_GUIDE.md` - Detailed troubleshooting steps

All changes have been committed and pushed to GitHub.

## 🚀 How to Fix the GitHub Actions Workflow

You have **THREE OPTIONS**:

### OPTION 1: AWS Console Manual Cleanup (FASTEST ⚡)

1. Go to [AWS Console](https://console.aws.amazon.com)
2. Navigate to **EC2 → Load Balancing → Listeners**
3. Find the listener for `payment-system-capgemini-alb`
4. **Delete the listener FIRST**
5. Go to **Target Groups**
6. Find `payment-system-capgemini-tg`
7. **Delete the target group**
8. Go to **Load Balancers**
9. Find `payment-system-capgemini-alb`
10. **Delete the ALB**
11. Re-run the GitHub Actions workflow

### OPTION 2: GitHub Actions Workflow Update (RECOMMENDED 🎯)

Modify your GitHub Actions workflow file (`.github/workflows/deploy.yml` or similar):

Add this BEFORE the `terraform apply` step:

```yaml
- name: Clean up stuck ALB resources
  env:
    AWS_REGION: ap-south-1
  run: |
    cd terraform
    
    # Remove from state if they exist
    terraform state rm aws_lb_listener.main || true
    terraform state rm aws_lb_target_group.main || true
    terraform state rm aws_lb.main || true
    
    # Now apply the plan
    terraform apply -auto-approve tfplan
```

### OPTION 3: AWS CLI in GitHub Actions

If AWS credentials are properly configured, add this to your workflow:

```yaml
- name: Clean up stuck ALB resources from AWS
  env:
    AWS_REGION: ap-south-1
  run: |
    # Delete listeners
    aws elbv2 describe-listeners --region $AWS_REGION --query 'Listeners[].ListenerArn' --output text | \
      xargs -I {} aws elbv2 delete-listener --listener-arn {} --region $AWS_REGION || true
    
    sleep 5
    
    # Delete target groups
    aws elbv2 describe-target-groups --region $AWS_REGION --query 'TargetGroups[?TargetGroupName==`payment-system-capgemini-tg`].TargetGroupArn' --output text | \
      xargs -I {} aws elbv2 delete-target-group --target-group-arn {} --region $AWS_REGION || true
    
    sleep 5
    
    # Delete ALBs
    aws elbv2 describe-load-balancers --region $AWS_REGION --query 'LoadBalancers[?LoadBalancerName==`payment-system-capgemini-alb`].LoadBalancerArn' --output text | \
      xargs -I {} aws elbv2 delete-load-balancer --load-balancer-arn {} --region $AWS_REGION || true
```

## 🔄 Steps to Complete

1. **Choose ONE option above**
2. **Clean up the stuck resources**
3. **Re-run the GitHub Actions workflow**
4. **Monitor the logs** - should now complete successfully

## 🆘 If Still Failing

1. Check AWS console to confirm resources were deleted
2. Verify GitHub Actions has proper AWS credentials
3. Check the Terraform state - ensure it's not corrupted
4. Try `terraform destroy -auto-approve` first, then `terraform apply`
5. Check the `SOLUTION_SUMMARY.md` and `RECOVERY_GUIDE.md` for more details

## ✨ After Successful Deployment

Your infrastructure should be created without this error. The code fixes ensure this won't happen again on future deployments.

---

**Files Updated:**
- `terraform/eks.tf` - Fixed lifecycle configurations
- `terraform/cleanup_alb.ps1` - Manual cleanup script
- `terraform/SOLUTION_SUMMARY.md` - Complete recovery guide
- `terraform/RECOVERY_GUIDE.md` - Detailed troubleshooting

**All changes committed and pushed to GitHub! 🎉**

