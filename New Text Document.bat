# ============ PHẦN CẤU HÌNH - CHẠY 1 LẦN ============
$projectName = "BE-EVENT-MNG-V3"
$githubUsername = "NguynNe3110"  # Thay bằng username GitHub của bạn
$githubEmail = "nguyen12112005@gmail.com"  # Thay bằng email GitHub của bạn
$repoName = "be-event-mng-v3
"

# Tạo thư mục và init repo
New-Item -ItemType Directory -Path $projectName -Force
Set-Location $projectName
git init

# Cấu hình user (QUAN TRỌNG: phải trùng email GitHub)
git config user.name $githubUsername
git config user.email $githubEmail
git config init.defaultBranch main

# Tạo file ban đầu
$fileName = "SeedCommit.kt"
New-Item $fileName -ItemType File -Force
Set-Content -Path $fileName -Value "// Initial commit"
git add $fileName
git commit -m "Initial commit"

# Tạo repo trên GitHub (dùng GitHub CLI nếu đã cài, hoặc tạo thủ thuật trên web)
# gh repo create $repoName --public --source=. --push
# Nếu chưa cài gh CLI, tạo repo trên github.com rồi chạy:
git remote add origin "https://github.com/$githubUsername/$repoName.git"
git branch -M main
git push -u origin main

# ============ PHẦN SEED COMMIT ============
$numCommits = 100

for ($i = 1; $i -le $numCommits; $i++) {
    Add-Content -Path $fileName -Value "// Seed commit number $i - $(Get-Date -Format 'yyyy-MM-dd')"

    $daysAgo = Get-Random -Minimum 0 -Maximum 365
    $commitDate = (Get-Date).AddDays(-$daysAgo).ToString("yyyy-MM-ddTHH:mm:ss")

    git add $fileName
    
    $env:GIT_AUTHOR_DATE = $commitDate
    $env:GIT_COMMITTER_DATE = $commitDate
    
    git commit -m "feat: seed commit $i for MVVM project"
}

Remove-Item Env:\GIT_AUTHOR_DATE
Remove-Item Env:\GIT_COMMITTER_DATE

# Push tất cả commit giả lên GitHub
git push origin main
Write-Host "✅ Đã seed và push xong $numCommits commits!" -ForegroundColor Green