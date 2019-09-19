dos2unix */*sh
python local.py test 
cd docker 
docker-compose -f dedution.yml down
docker-compose -f dedution.yml up -d 
