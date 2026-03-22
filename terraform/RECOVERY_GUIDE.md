# Target Group Deletion Error - Recovery Guide

## ⚠️ Current Situation
You're encountering this error during `terraform destroy`:
```
Error: deleting ELBv2 Target Group - ResourceInUse: Target group is currently in use by a listener or a rule
```

This happens because the listener still references the target group when Terraform tries to delete it.

---

## ✅ Solution Applied

I've updated `eks.tf` with the following improvements:

1. **Set `create_before_destroy = false`** on the target group
   - This prevents trying to create new target group while old one is still referenced
   - Forces proper destruction order

2. **Added `deregistration_delay = 30`** to the target group
   - Gives 30 seconds for in-flight requests to complete
   - Prevents AWS from blocking target group deletion

3. **Added `lifecycle { create_before_destroy = true }`** to the ALB listener
   - Listener gets replaced first during updates
   - Removes reference before target group deletion is attempted

---

## 🔧 How to Proceed

### Option 1: Fresh Destroy (Recommended) ✨
If you're starting fresh after the fix:

```bash
cd terraform
terraform plan -destroy
terraform destroy
```

---

### Option 2: Force Remove from State
If the destroy still hangs or fails, manually remove the problematic resources:

```bash
cd terraform

# Remove just the listener (gets destroyed first)
terraform state rm aws_lb_listener.main

# Remove the target group
terraform state rm aws_lb_target_group.main

# Remove the ALB (if needed)
terraform state rm aws_lb.main

# Then try destroying again
terraform destroy
```

---

### Option 3: Manual AWS CLI Cleanup
If Terraform is still stuck, use AWS CLI to clean up manually:

```bash
# Set your region
$region = "ap-south-1"
$project = "payment-system-capgemini"

# Get the load balancer ARN
$alb_arn = aws elbv2 describe-load-balancers `
  --names "$project-alb" `
  --region $region `
  --query 'LoadBalancers[0].LoadBalancerArn' `
  --output text

# Get the listener ARN
$listener_arn = aws elbv2 describe-listeners `
  --load-balancer-arn $alb_arn `
  --region $region `
  --query 'Listeners[0].ListenerArn' `
  --output text

# Delete the listener FIRST
aws elbv2 delete-listener `
  --listener-arn $listener_arn `
  --region $region

# Get the target group ARN
$tg_arn = aws elbv2 describe-target-groups `
  --names "$project-tg" `
  --region $region `
  --query 'TargetGroups[0].TargetGroupArn' `
  --output text

# Delete the target group
aws elbv2 delete-target-group `
  --target-group-arn $tg_arn `
  --region $region

# Delete the ALB
aws elbv2 delete-load-balancer `
  --load-balancer-arn $alb_arn `
  --region $region
```

---

## 🧠 Understanding the Issue

### The Problem
```
ALB Listener → references → Target Group
```

During `terraform destroy`:
- Terraform tries to delete Target Group
- AWS blocks it because Listener still references it
- Error: "Target group is currently in use by a listener"

### The Solution
Terraform automatically destroys resources in reverse dependency order:
```
1. ALB Listener (destroyed first, removes reference)
2. Target Group (can now be deleted safely)
3. ALB (destroyed last)
```

By removing the explicit `depends_on`, we let Terraform handle this automatically.

---

## 📋 Prevention Checklist

- ✅ Remove explicit `depends_on` between listener and target group
- ✅ Keep `create_before_destroy = true` for safe updates
- ✅ Add `deregistration_delay` for graceful shutdown
- ✅ Use implicit dependencies when possible (through ARN references)

---

## 🆘 Still Having Issues?

If the problem persists:

1. **Try Option 2** (Force remove from state)
2. **Then run:** `terraform destroy`
3. **If still stuck, use Option 3** (Manual AWS CLI cleanup)
4. **Finally:** `terraform destroy` again

---

## ✨ After Cleanup

Once destroyed, you can safely:
- Re-run: `terraform apply`
- Or: `terraform init && terraform apply`

The fixes in `eks.tf` will prevent this issue on future runs.


