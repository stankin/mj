# Install software on server using Ansible

prepare servers

    ansible-galaxy collection install middleware_automation.jcliff
    ansible-playbook ./ansible/setup-playbook.yaml -i ./ansible/inventories/test/inventory.yaml

then you can set up the tunnel and 

    ssh -N -L localhost:9990:localhost:9990 root@$(ansible-inventory -i ansible/inventories/test/inventory.yaml --list test |  jq -r '._meta.hostvars.test.ansible_host')

deploy via maven over ssh:

    mvn wildfly:deploy -DskipTests -Dwildfly.hostname=localhost -Dwildfly.username=deployer -Dwildfly.password=thoht2Xae#ca