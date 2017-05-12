import boto3
import os
import zipfile

src='.\\downloaded_logs\\'
#we are creating a list of csv files
localFiles = list()
for dirname, subdirs, files in os.walk(src):
	for filename in files:
		absname = os.path.abspath(os.path.join(dirname, filename))
		if os.path.splitext(absname)[1][1:] == 'csv':
			localFiles.append(absname)
		
#log in to aws and get a list of log files there
awsFiles = list()
session = boto3.Session()
s3 = session.resource('s3')
bucket = s3.Bucket('frc1736logfiles')
for key in bucket.objects.all():
	awsFiles.append(key)
	
#we are finding log files missing from aws
uploadFiles = list()
for lFile in localFiles:
	fn=os.path.basename(lFile)
	fn.replace('.csv','.zip')
	if not fn in awsFiles:
		uploadFiles.append(lFile)
		
#zip up csv files and upload to aws
for lFile in uploadFiles:
	zipfileName = lFile.replace('.csv','.zip')
	zf = zipfile.ZipFile(zipfileName, "w", zipfile.ZIP_DEFLATED)
	fn=os.path.basename(lFile)
	zf.write(lFile, fn)
	zf.close()
	data=open(zipfileName,'rb')
	bucket.put_object(Key=os.path.basename(zipfileName), Body=data, ACL='public-read')
	print('uploading file: ' + zipfileName)
	data.close()
	os.remove(zipfileName)
	
input("press enter to continue...")