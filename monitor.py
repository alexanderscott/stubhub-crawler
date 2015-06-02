import time, os, smtplib
from email.mime.text import MIMEText
import mail_config

from_addr = mail_config.email
to_addr = mail_config.email

filename = 'stubhub.log'
file = open(filename, 'r')

st_results = os.stat(filename)
st_size = st_results[6]
file.seek(st_size)

def send_email(subj, body):
	msg = MIMEText(body)
	msg['Subject'] = subj
	msg['From'] = from_addr
	msg['To'] = to_addr

	s = smtplib.SMTP(mail_config.smtp_server, 587)
	s.ehlo()
	s.starttls()
	s.login(mail_config.user, mail_config.password)
	s.sendmail(from_addr, to_addr, msg.as_string())
	s.quit()

def on_line(line):
        if 'ERROR' in line:
                send_email('Stubhub ERROR!', line)

while 1:
	where = file.tell()
	line = file.readline()
	if not line:
		time.sleep(1)
		file.seek(where)
	else:
		on_line(line)
