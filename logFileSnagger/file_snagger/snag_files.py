################################################################################
#   COPYRIGHT [C] 2016 FRC1736 Robot Casserole 
#
#   FileName: snag_files.py
#
#   Description: 
#       Script to connect to an FRC roboRIO via FTP, and grab all log files
#       generated into a known directory.
#
#   Assumptions:
#       - Python 3.4 or later used
#       - Windows cmd shell is used
#       - Connected to same wifi network 
#       - Adjust "Team Number" string below to match your team
#
################################################################################

from ftplib import FTP
import sys, os, time

TEAM_NUMBER_STR = '1736'
RIO_ADDRESS = 'roboRIO-'+TEAM_NUMBER_STR+'-FRC.local'
RIO_LOG_FPATH = '/media/sda1/data_captures/'
LOCAL_PATH = '.\\downloaded_logs\\'
LOG_LOGS_DIR = '.\\file_snagger\\logs\\'

print('*************************************************************')
print('*****         FRC '+TEAM_NUMBER_STR+' Robot Log File Snagger           *****')
print('*************************************************************')




def log_log_result(fsnag_list):
    if not os.path.isdir(LOG_LOGS_DIR):
        os.mkdir(LOG_LOGS_DIR)
    
    if(fsnag_list == None):
        fname = os.path.join(LOG_LOGS_DIR, "FAILED_"+time.strftime("%Y%m%d-%H%M%S")+"_log.txt")
        fcontents = "ERROR! Snag failed! No logs grabbed!"
    else:
        fname = os.path.join(LOG_LOGS_DIR, "SNAGGED_"+time.strftime("%Y%m%d-%H%M%S")+"_log.txt")
        fcontents = ["Files were snagged:\n\n"]
        fcontents.extend([x + " \n" for x in fsnag_list])
        fcontents = "".join(fcontents)
        
    tempf = open(fname, "w")
    tempf.write(fcontents)
    tempf.close()
    return

# Attempt an FTP connection. Retry if we can't. User may do a ctrl-c to exit if needed.
failed = 1;
while(failed == 1):
    print('Attempting to connect to roboRIO on ftp://' + RIO_ADDRESS + " ...")
    failed = 0;
    try:
        ftp = FTP(RIO_ADDRESS)
        ftp.login()
    except:
        failed = 1;
        print('Error - could not connect to roboRIO! Retrying...')
    
# FTP Connected! Change to correct working proper working directory
# and snag the list of files
ftp.cwd(RIO_LOG_FPATH)
filenames = ftp.nlst() # get filenames within the directory

# Inform user what files were found
print("Found "+ str(len(filenames)) +" Files.")

#make local dir if doesn't exist yet.
if not os.path.isdir(LOCAL_PATH):
    os.mkdir(LOCAL_PATH)

# Copy each file over
i = 1
for filename in filenames:
    print("Copying "+ filename + " (" + str(i) + "/" + str(len(filenames)) + ") ")
    local_filename = os.path.join(LOCAL_PATH, filename)
    file = open(local_filename, 'wb')
    ftp.retrbinary('RETR '+ filename, file.write)
    file.close()
    ftp.delete(filename)
    i = i+1
    
#We're Done!
ftp.quit()
log_log_result(filenames)
sys.exit(0)