# Windows PowerShell equivalent of create-prod-secret.sh
param(
    [string]$ParamName = "/api-test-framework/prod/api-password",
    [string]$Region = "ap-south-1"
)
$secure = Read-Host "Prod API password" -AsSecureString
$plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))

aws ssm put-parameter --region $Region --name $ParamName --type SecureString `
    --value $plain --overwrite `
    --description "Prod API password used by the REST Assured test framework"

Write-Host "Stored $ParamName in $Region"
