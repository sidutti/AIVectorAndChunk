cd frontend
ng build --configuration production
cd ..
cp -r frontend/dist backend/src/main/resources/static
git add .
git commit -m 'upgrade data'
git push
