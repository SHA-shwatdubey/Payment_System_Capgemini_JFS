# List VPCs and identify which ones can be deleted
$region = "ap-south-1"

Write-Host "=== VPC Inventory for $region ===" -ForegroundColor Cyan
Write-Host ""

# Get all VPCs
$vpcs = aws ec2 describe-vpcs --region $region --output json | ConvertFrom-Json

if ($vpcs.Vpcs.Count -eq 0) {
    Write-Host "No VPCs found in region: $region" -ForegroundColor Yellow
    exit 0
}

Write-Host "Total VPCs: $($vpcs.Vpcs.Count)/5 (AWS Limit)" -ForegroundColor Yellow
Write-Host ""

# Display each VPC with details
foreach ($vpc in $vpcs.Vpcs) {
    $vpcId = $vpc.VpcId
    $cidr = $vpc.CidrBlock
    $isDefault = $vpc.IsDefault
    $state = $vpc.State
    $tags = $vpc.Tags

    $name = "N/A"
    if ($tags) {
        $nameTag = $tags | Where-Object { $_.Key -eq "Name" }
        if ($nameTag) {
            $name = $nameTag.Value
        }
    }

    Write-Host "VPC ID: $vpcId" -ForegroundColor Green
    Write-Host "  Name: $name"
    Write-Host "  CIDR: $cidr"
    Write-Host "  Default: $isDefault"
    Write-Host "  State: $state"

    # Check for associated resources
    $subnets = aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpcId" --region $region --query 'Subnets | length(@)' --output text
    $instances = aws ec2 describe-instances --filters "Name=vpc-id,Values=$vpcId" "Name=instance-state-name,Values=running,stopped" --region $region --query 'Reservations | length(@)' --output text
    $natGateways = aws ec2 describe-nat-gateways --filter "Name=vpc-id,Values=$vpcId" --region $region --query 'NatGateways | length(@)' --output text

    Write-Host "  Resources:"
    Write-Host "    - Subnets: $subnets"
    Write-Host "    - Instances: $instances"
    Write-Host "    - NAT Gateways: $natGateways"

    if ($isDefault -eq $true) {
        Write-Host "  Status: [DEFAULT VPC] - DO NOT DELETE" -ForegroundColor Yellow
    } elseif ($subnets -eq 0 -and $instances -eq 0 -and $natGateways -eq 0) {
        Write-Host "  Status: [SAFE TO DELETE] - No resources" -ForegroundColor Green
    } else {
        Write-Host "  Status: [IN USE] - Keep or clean resources first" -ForegroundColor Magenta
    }

    Write-Host ""
}

Write-Host "=== Summary ===" -ForegroundColor Cyan
$defaultCount = ($vpcs.Vpcs | Where-Object { $_.IsDefault -eq $true }).Count
$customCount = ($vpcs.Vpcs | Where-Object { $_.IsDefault -eq $false }).Count

Write-Host "Default VPCs: $defaultCount (cannot delete)" -ForegroundColor Yellow
Write-Host "Custom VPCs: $customCount (can delete if not in use)" -ForegroundColor Yellow
Write-Host ""

if ($customCount -gt 0) {
    $unusedCount = 0
    foreach ($vpc in ($vpcs.Vpcs | Where-Object { $_.IsDefault -eq $false })) {
        $subnets = aws ec2 describe-subnets --filters "Name=vpc-id,Values=$($vpc.VpcId)" --region $region --query 'Subnets | length(@)' --output text
        $instances = aws ec2 describe-instances --filters "Name=vpc-id,Values=$($vpc.VpcId)" "Name=instance-state-name,Values=running,stopped" --region $region --query 'Reservations | length(@)' --output text
        $natGateways = aws ec2 describe-nat-gateways --filter "Name=vpc-id,Values=$($vpc.VpcId)" --region $region --query 'NatGateways | length(@)' --output text

        if ($subnets -eq 0 -and $instances -eq 0 -and $natGateways -eq 0) {
            $unusedCount++
        }
    }

    if ($unusedCount -gt 0) {
        Write-Host "Unused VPCs (safe to delete): $unusedCount" -ForegroundColor Green
        Write-Host "Action: Delete $unusedCount unused VPC(s) to free up space" -ForegroundColor Green
    } else {
        Write-Host "No unused VPCs found" -ForegroundColor Yellow
        Write-Host "Action: Request AWS limit increase OR clean up resources in existing VPCs" -ForegroundColor Yellow
    }
}

