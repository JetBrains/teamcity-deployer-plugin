# FTP Servers

## Setting up a server for testing

### Prerequisites: VirtualBox, Vagrant

1. Vagrantfile
```vagrantfile
Vagrant.configure("2") do |config|
  config.vm.box = "generic/debian12"
  config.vm.hostname = "debian-bridge"

  config.vm.network "public_network", bridge: "en0", use_dhcp_assigned_default_route: true

  config.vm.provider "virtualbox" do |vb|
    vb.name = "DebianBridge"
    vb.memory = "512"
    vb.cpus = 1
  end
end
```

2. Setting up:
```bash

sudo apt update
sudo apt install vsftpd -y
sudo mkdir -p /etc/ssl/private
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/ssl/private/vsftpd.key \
  -out /etc/ssl/private/vsftpd.crt
sudo chown root:root /etc/ssl/private/vsftpd.key /etc/ssl/private/vsftpd.crt
sudo chmod 600 /etc/ssl/private/vsftpd.key
sudo chmod 644 /etc/ssl/private/vsftpd.crt

sudo adduser --home /home/ftpuser --shell /bin/bash ftpuser
sudo mkdir -p /home/ftpuser
sudo chown ftpuser:ftpuser /home/ftpuser
sudo chmod 755 /home/ftpuser

sudo mkdir -p /srv/ftp/upload

sudo chown root:root /srv/ftp
sudo chmod 755 /srv/ftp

sudo chown ftp:ftp /srv/ftp/upload
sudo chmod 777 /srv/ftp/upload

sudo systemctl restart vsftpd
sudo systemctl enable vsftpd

lftp -u ftpuser:ftpuser -e "set ftp:ssl-force true; set ssl:verify-certificate no; ls" ftp://192.168.188.196
```

3. Files:
```/etc/vsftpd.conf

listen=YES
listen_ipv6=NO
anonymous_enable=YES
local_enable=YES
write_enable=YES
chroot_local_user=YES
anon_upload_enable=YES
anon_mkdir_write_enable=YES
anon_root=/srv/ftp

# TLS / SSL
ssl_enable=YES
allow_anon_ssl=NO
force_local_data_ssl=YES
force_local_logins_ssl=YES
ssl_tlsv1=YES
ssl_sslv2=NO
ssl_sslv3=NO
rsa_cert_file=/etc/ssl/private/vsftpd.crt
rsa_private_key_file=/etc/ssl/private/vsftpd.key

pasv_enable=YES
pasv_min_port=30000
pasv_max_port=30100
port_enable=YES
pasv_address=192.168.188.196

require_ssl_reuse=NO
ssl_ciphers=HIGH

xferlog_enable=YES
require_ssl_reuse=NO

allow_writeable_chroot=YES
secure_chroot_dir=/home/ftpuser
```

