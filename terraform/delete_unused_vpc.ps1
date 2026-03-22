$ErrorActionPreference = "Stop"

$region = "ap-south-1"
$vpcId = "vpc-047c77f486eb21995"

Write-Host "Deleting unused VPC: $vpcId" -ForegroundColor Cyan

# Safety checks
$vpcExists = aws ec2 describe-vpcs --region $region --vpc-ids $vpcId --query 'length(Vpcs)' --output text
if ($vpcExists -eq "0") {
  Write-Host "VPC not found, nothing to delete." -ForegroundColor Yellow
  exit 0
}

$isDefault = aws ec2 describe-vpcs --region $region --vpc-ids $vpcId --query 'Vpcs[0].IsDefault' --output text
if ($isDefault -eq "True") {
  throw "Refusing to delete default VPC"
}

$eniCount = aws ec2 describe-network-interfaces --region $region --filters "Name=vpc-id,Values=$vpcId" --query 'length(NetworkInterfaces)' --output text
if ($eniCount -ne "0") {
  throw "VPC has $eniCount network interfaces in use; aborting delete"
}

# Delete NAT gateways (if any)
function Get-IdsFromText($text) {
  if ([string]::IsNullOrWhiteSpace($text)) { return @() }
  return ($text -split "\s+") | Where-Object { $_ -and $_ -ne "None" }
}

$natText = aws ec2 describe-nat-gateways --region $region --filter "Name=vpc-id,Values=$vpcId" --query 'NatGateways[].NatGatewayId' --output text
$natIds = Get-IdsFromText $natText
foreach ($natId in $natIds) {
  aws ec2 delete-nat-gateway --region $region --nat-gateway-id $natId | Out-Null
}

# Delete route table associations (non-main) and non-main route tables
$routeTables = aws ec2 describe-route-tables --region $region --filters "Name=vpc-id,Values=$vpcId" --output json | ConvertFrom-Json
foreach ($rt in $routeTables.RouteTables) {
  foreach ($assoc in $rt.Associations) {
    if ($assoc.RouteTableAssociationId -and -not $assoc.Main) {
      aws ec2 disassociate-route-table --region $region --association-id $assoc.RouteTableAssociationId | Out-Null
    }
  }
}
foreach ($rt in $routeTables.RouteTables) {
  $mainAssoc = $rt.Associations | Where-Object { $_.Main -eq $true }
  if (-not $mainAssoc) {
    aws ec2 delete-route-table --region $region --route-table-id $rt.RouteTableId | Out-Null
  }
}

# Detach and delete internet gateways
$igwText = aws ec2 describe-internet-gateways --region $region --filters "Name=attachment.vpc-id,Values=$vpcId" --query 'InternetGateways[].InternetGatewayId' --output text
$igwIds = Get-IdsFromText $igwText
foreach ($igwId in $igwIds) {
  aws ec2 detach-internet-gateway --region $region --internet-gateway-id $igwId --vpc-id $vpcId | Out-Null
  aws ec2 delete-internet-gateway --region $region --internet-gateway-id $igwId | Out-Null
}

# Delete subnets
$subnetText = aws ec2 describe-subnets --region $region --filters "Name=vpc-id,Values=$vpcId" --query 'Subnets[].SubnetId' --output text
$subnetIds = Get-IdsFromText $subnetText
foreach ($subnetId in $subnetIds) {
  aws ec2 delete-subnet --region $region --subnet-id $subnetId | Out-Null
}

# Delete non-default network ACLs
$naclText = aws ec2 describe-network-acls --region $region --filters "Name=vpc-id,Values=$vpcId" --query 'NetworkAcls[?IsDefault==`false`].NetworkAclId' --output text
$naclIds = Get-IdsFromText $naclText
foreach ($naclId in $naclIds) {
  aws ec2 delete-network-acl --region $region --network-acl-id $naclId | Out-Null
}

# Delete non-default security groups
$sgText = aws ec2 describe-security-groups --region $region --filters "Name=vpc-id,Values=$vpcId" --query 'SecurityGroups[?GroupName!=`default`].GroupId' --output text
$sgIds = Get-IdsFromText $sgText
foreach ($sgId in $sgIds) {
  aws ec2 delete-security-group --region $region --group-id $sgId | Out-Null
}

# Delete VPC
aws ec2 delete-vpc --region $region --vpc-id $vpcId | Out-Null

Write-Host "Deleted VPC: $vpcId" -ForegroundColor Green
Write-Host "Remaining VPC count:" -NoNewline
aws ec2 describe-vpcs --region $region --query 'length(Vpcs)' --output text
