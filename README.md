# Task 11 - Gruppo 41
Componenti:
- Luigi Cecere - M63/1413
- Chiara Capocello - M63/1451
- Giovanni Gentile - M63/1450
- Michelangelo Formato - M63/1519

# GUIDA ALL'INSTALLAZIONE

## PASSO 1
Scaricare Docker Desktop per il proprio sistema operativo.

## PASSO 2
Si deve avviare lo script "installer.bat" se si sta usando una distribuzione Windows oppure "installermac.sh" nel caso si utilizzi macOS o una distro di Linux. Tali script dovranno essere avviati unicamnete con Docker in esecuzione, altrimenti l'installazione non partirà. Saranno effettuate le seguenti operazioni:
1) Creazione della rete "global-network" comune a tutti i container.
2) Creazione del volume "VolumeT9" comune ai Task 1 e 9 e del volume "VolumeT8" comune ai Task 1 e 8.
3) Creazione dei singoli container in Docker desktop.

NOTA: il container relativo al Task 9 ("Progetto-SAD-G19-master") si sospenderà autonomamente dopo l'avvio. Esso viene utilizzato solo per "popolare" il volume "VolumeT9" condiviso con il Task 1.

## PASSO 3
Si deve configurare il container "manvsclass-mongo_db-1" così come descritto anche nella documentazione del Task 1.
Per fare ciò bisogna fare le seguenti operazioni:
1) Posizionarsi all'interno del terminale del container
2) Digitare il comando "mongosh"
3) Digitare i seguenti comandi:

        use manvsclass
        db.createCollection("ClassUT");
        db.createCollection("interaction");
        db.createCollection("Admin");
        db.createCollection("Operation");
        db.ClassUT.createIndex({ difficulty: 1 })
        db.Interaction.createIndex({ name: "text", type: 1 })
        db.interaction.createIndex({ name: "text" })
        db.Admin.createIndex({username: 1})

L'intera applicazione è adesso pienamente configurata e raggiungibile sulla porta :80. Per una guida all'installazione e all'utilizzo più completa consultare la documentazione al capitolo 8.

# VIDEO DIMOSTRAZIONE
## Admin


![Guarda il mio video](Video/Admin.mp4)



## Player

![Guarda il mio video](Video/player.mp4)
