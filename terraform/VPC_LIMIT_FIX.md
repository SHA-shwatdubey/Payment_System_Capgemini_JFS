# VPC Limit Exceeded - Resolution Guide

## ⚠️ Error Details

```
Error: creating EC2 VPC: operation error EC2: CreateVpc
VpcLimitExceeded: The maximum number of VPCs has been reached
```

## 🔍 Root Cause

AWS limits you to **5 VPCs per region** by default (in most regions). Your account has reached this limit in `ap-south-1`.

## ✅ Solutions

### SOLUTION 1: Delete Unused VPCs (RECOMMENDED ⚡)

1. **Go to AWS Console**
   - Navigate to: `VPC Dashboard → Your VPCs`
   - Region: Select `ap-south-1`

2. **Find unused VPCs**
   - Look for VPCs that are not in use
   - Common candidates: old test VPCs, failed deployments, etc.

3. **Delete a VPC** (Follow this order):
   - Select the VPC
   - Go to "Actions" → "Delete VPC"
   - AWS will help you delete associated resources:
     - Subnets
     - Route tables
     - Internet gateways
     - NAT gateways
     - Network ACLs
   - Confirm deletion

4. **After deletion**
   - Wait 1-2 minutes
   - Re-run the GitHub Actions workflow

### SOLUTION 2: Request AWS Limit Increase 🎫

If all VPCs are in use:

1. **Go to AWS Console**
   - Service Quotas → EC2
   - Search for "VPC"
   - Find "VPCs per region"

2. **Request quota increase**
   - Click the quota
   - Click "Request quota increase"
   - Set new desired count (e.g., 10)
   - Submit request

3. **Wait for approval**
   - Usually approved within minutes
   - Once approved, re-run workflow

### SOLUTION 3: Use Existing VPC 🔄

If you want to reuse an existing VPC in your Terraform:

Edit `terraform/variables.tf`:
```hcl
variable "vpc_id" {
  description = "Existing VPC ID to use"
  type        = string
  default     = "vpc-xxxxxxxx"  # Replace with your VPC ID
}
```

Edit `terraform/main.tf`:
```hcl
# Instead of creating new VPC, use existing:
data "aws_vpc" "existing" {
  id = var.vpc_id
}

# Then reference as: data.aws_vpc.existing.id
```

## 🛠️ How to Check Your VPCs

Run these AWS CLI commands to see what's deployed:

```bash
# List all VPCs in ap-south-1
aws ec2 describe-vpcs --region ap-south-1 --query 'Vpcs[].{VpcId:VpcId, CidrBlock:CidrBlock, Tags:Tags}' --output table

# List VPCs with their associated resources
aws ec2 describe-vpcs --region ap-south-1 --query 'Vpcs[].[VpcId, CidrBlock, State]' --output table

# Find orphaned/unused VPCs (no instances, no NAT gateways, etc.)
aws ec2 describe-vpcs --region ap-south-1 --filters "Name=isDefault,Values=false" --query 'Vpcs[].VpcId' --output text
```

## 📊 VPC Limit Details

| Aspect | Details |
|--------|---------|
| Default Limit | 5 VPCs per region |
| Region | ap-south-1 (Asia Pacific Mumbai) |
| Current Status | **REACHED (5/5)** |
| Solution | Delete 1 VPC or request increase |
| Time to Fix | 2-30 minutes |

## 🚀 Quick Fix Steps

1. **Fastest (Delete unused VPC)**
   ```
   ✓ Go to AWS Console
   ✓ VPC Dashboard → Your VPCs
   ✓ Delete 1 unused VPC
   ✓ Re-run workflow (2 min)
   ```

2. **Alternative (Request increase)**
   ```
   ✓ Service Quotas → VPC limit
   ✓ Request increase
   ✓ Wait for approval (~5-30 min)
   ✓ Re-run workflow
   ```

## 📝 Checklist Before Retry

- [ ] Confirmed you're in the correct region (ap-south-1)
- [ ] Identified which VPC to delete
- [ ] Deleted the VPC and waited 1-2 minutes
- [ ] Verified VPC is gone from console
- [ ] Ready to re-run GitHub Actions workflow

## 🆘 Common Issues

**Q: How do I know which VPC is safe to delete?**
A: Look for VPCs with:
- No instances running
- No subnets with resources
- Old creation dates from previous test runs

**Q: What if I delete the wrong VPC?**
A: You can recreate it or contact AWS support. Associated resources are deleted too.

**Q: How long does limit increase take?**
A: Usually 5-30 minutes, sometimes instant.

**Q: Should I delete the current deployment VPC?**
A: NO! Only delete OLD/unused VPCs from previous attempts.

## 🔑 Next Steps

1. Choose Solution 1 or 2 above
2. Complete the deletion/request
3. Re-run the GitHub Actions workflow
4. Your infrastructure will deploy successfully

---

**Estimated Time to Fix: 2-5 minutes** ⚡

