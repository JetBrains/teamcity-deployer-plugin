#!/bin/bash


PARAMS=''
while IFS='=' read -r -d '' n v; do
  if [[ $n = DEF_* ]]
  then
    PARAMS="$PARAMS -D${n:4}=$v"
  fi
done < <(env -0)
echo $PARAMS

echo "gpp $PARAMS /tmp/vsftp.gpp.conf > /etc/vsftpd/vsftp.conf"
gpp $PARAMS /tmp/vsftp.gpp.conf > /etc/vsftpd/vsftp.conf

echo 2
echo "Generating self-signed certificate"
mkdir -p /etc/vsftpd/private

echo 3

openssl req -x509 -nodes -days 7300 \
    -newkey rsa:2048 -keyout /etc/vsftpd/private/vsftpd.pem -out /etc/vsftpd/private/vsftpd.pem \
    -subj "/C=FR/O=My company/CN=${DEF_DOMAIN-localhost}"

echo 4
openssl pkcs12 -export -out /etc/vsftpd/private/vsftpd.pkcs12 -in /etc/vsftpd/private/vsftpd.pem -passout pass:
echo 5

chmod 755 /etc/vsftpd/private/vsftpd.pem
chmod 755 /etc/vsftpd/private/vsftpd.pkcs12
echo 6

&>/dev/null /usr/sbin/vsftpd /etc/vsftpd/vsftp.conf