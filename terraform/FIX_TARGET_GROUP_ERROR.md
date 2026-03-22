# Fix for Target Group Deletion Error

## Problem
```
Error: deleting ELBv2 Target Group - ResourceInUse: Target group is currently in use by a listener or a rule
```

## Root Cause
The target group is still being referenced by the ALB listener during destruction. Terraform was trying to delete the target group before removing the listener reference.

## Solution Applied
I've updated the Terraform configuration with the following improvements:

### Changes Made to `eks.tf`:

1. **Added `lifecycle.create_before_destroy` to ALB** - This ensures the ALB is recreated before the old one is destroyed if needed
2. **Added `lifecycle.create_before_destroy` to Target Group** - This allows the target group to be replaced safely
3. **Added `depends_on` to the ALB Listener** - This explicitly tells Terraform that the listener depends on the target group, which helps with destruction order

## To Fix the Current Issue:

If you still have the error from a previous run, you may need to manually clean up the AWS resources:

### Option 1: Terraform Refresh (Recommended First Step)
```bash
cd terraform
terraform refresh
terraform plan
```

### Option 2: Manual AWS Cleanup
If Terraform refresh doesn't work, manually remove the listener first:

```bash
# Get the listener ARN
aws elbv2 describe-listeners \
  --load-balancer-arn "arn:aws:elasticloadbalancing:ap-south-1:013141018043:loadbalancer/app/payment-system-capgemini-alb/..." \
  --region ap-south-1

# Delete the listener (use the listener ARN from above)
aws elbv2 delete-listener \
  --listener-arn "arn:aws:elasticloadbalancing:ap-south-1:013141018043:listener/app/payment-system-capgemini-alb/.../..." \
  --region ap-south-1

# Then delete the target group
aws elbv2 delete-target-group \
  --target-group-arn "arn:aws:elasticloadbalancing:ap-south-1:013141018043:targetgroup/payment-system-capgemini-tg/a6aee61ece381daf" \
  --region ap-south-1
```

### Option 3: Remove from Terraform State
If the resources still conflict, you can remove them from the state and let Terraform recreate them:

```bash
cd terraform
terraform state rm aws_lb_listener.main
terraform state rm aws_lb_target_group.main
terraform state rm aws_lb.main
terraform plan
terraform apply
```

## Prevention Going Forward
The changes I've made ensure that:
- Listeners are properly ordered to be destroyed before target groups
- Resources can be safely replaced with `create_before_destroy`
- Explicit dependencies prevent race conditions during destruction

## Next Steps
1. Run `terraform plan` to verify the changes
2. If there are still issues, use Option 2 or Option 3 above
3. Once cleared, run `terraform apply` with confidence

