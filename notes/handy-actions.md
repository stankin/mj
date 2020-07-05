Restore database

    xzcat pg_mj_dump-2018-03-25-04-00-01.sql.xz | psql mj -U mj
    
SSH tunnel to Wildfly web console

    ssh -N -L localhost:9990:localhost:9990 root@deb10.uits-labs.ru