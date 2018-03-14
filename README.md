# deployeur-maven-plugin
un plugin maven pour déployer une appli web java jee dans une cible déploiement docker/tomcat

# Comment utiliser le plugin?

## 1. construire une infrastruture cible de déploiement

Pour cela, il vous suffit d'utiliser:
https://github.com/Jean-Baptiste-Lasselle/lauriane
en suivant simplement les intructions https://github.com/Jean-Baptiste-Lasselle/lauriane/blob/master/ModeDemploi.pdf
Vous noterez les valeurs des paramètres suivants, (cf. ModeDemploi.pdf, et "monter-cible-deploiement.sh") :

* le nom du conteneur embarquant le serveur jee: 

           <nom-conteneur-docker-srv-jee></nom-conteneur-docker-srv-jee>
* l'adresse IP utilisée par le serveur Jee:

           <ip-cible-srv-jee></ip-cible-srv-jee>
* le numéro de port utilisé par le sereur Jee:

           <no-port-cible-srv-jee></no-port-cible-srv-jee>
* le nom de l'utilisateur Linux, opérateur pour le plugin maven:
       
           <lx-user></lx-user>
* le mot de passe de l'utilisateur Linux, opérateur pour le plugin maven :

           <lx-pwd></lx-pwd>
* nom et mot de passe de l'utilisateur linux opérateur pour le plguin maven
  sont donnés à la fin de la construction de la cible de déploiement, par la sortie standard, sous la forme:
  
  	  --------------------------------------------------------  
	  --- De plus, l'utilisateur linux que votre plugin  
	  --- doit utiliser est: 
	  --- 				 
	  --- 				nom d'utilisateur linux: $MVN_PLUGIN_OPERATEUR_LINUX_USER_NAME
	  --- 				 
	  --- 				mot de passe: $MVN_PLUGIN_OPERATEUR_LINUX_USER_PWD
	  --- 				 

Ces paramètres et d'autres seront à utiliser pour la configuration du deployeur-maven-plugin, dans le pom.xml d'une application web que nous voulons déployer, avec les balises indiquées ci-dessus.

Toutes ces valeurs sont rassemblées dans un fichier d'extrait xml généré par la construction de l'infrastructure:

	lauriane/config.deployeur.plugin.xml



## 2. Utiliser le plugin "deployeur-maven-plugin" pour déployer l'application web exemple

 ### - Avant d'exécuter le build maven de l'application web exemple, il nous faut le plugin "deployeur-maven-plugin"
   dans notre repo maven local. 
   Pour cela, nous allons cloner le code source du plugin, builder le plugin en l'installant dans le repo maven local:
   
              git clone https://github.com/Jean-Baptiste-Lasselle/deployeur-maven-plugin.git
              mvn clean install -up -U -f deployeur-maven-plugin/deployeur/pom.xml
   
   Il faut enfin savoir que ce plugin, pour réaliser les déploiements d'applications web jee, utilise deux éléments:
   
   * un "repo git de déploiement": Vous devrez créer cerepo. Dans ce repo, le plugin versionne simplement le fichier \*.war à déployer. Ce repo permettra dans une évolution future du plugin, de déployer plusieurs versions de l'application web simultanément. Cette évolution pourra utiliser le plugin maven "git-commit-id", pour inclure le numéro de commit de code source de l'aplication web, dans son nom de fichier d'artefact \*.war
    
   * un répertoire local à la machine exécutant l'IDE, il sera utilisé comme un repo local git par le plugin maven, afin de faire des commit &&  push des \*.wars produits à chaque build. À chaque build de l'application web, ce plugin commit et push le fichier \*.war produit, sur le "repo git de déploiement". Ce répertoire ne devant pas être utilisé par votre IDE, afin d'éviter conflits entre les actions git du plugin maven, et votre IDE. Ce répertoire est détruit et re-créée à chaque invocation du plugin.
  
   
 ### - Sur votre poste de dev., faîtes un git clone du repo contenant l'application web exemple à déployer:

              git clone https://github.com/Jean-Baptiste-Lasselle/lauriane-deployeur-test.git

 ### - Puis, avec Eclipse, ou votre IDE, importez le projet maven "Existing Maven Project...", pour créer un projet eclipse dans votre IDE.

              Utilisateurs d'Eclipse: faîtes un "Maven Update".

      
 ### - Avant d'exécuter le build maven de l'application web exemple, il nous faut enfin éditer la configuration du plugin dans le fichier:
   
             ./lauriane-deployeur-test/jiblWebappTest/pom.xml
   
 ### - Exécutez ensuite, avec eclipse, le build maven de l'application web exemple:

              mvn clean install -up -U -f ./lauriane-deployeur-test/jiblWebappTest/pom.xml

Dans ce fichier, la configuration typique contient les éléments suivants:

    <plugins>
    <plugin>
	<groupId>lasselle</groupId>
	<artifactId>deployeur</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<executions>
		<execution>
			<phase>install</phase>
			<goals>
				<goal>deploie</goal>
			</goals>
			<configuration>
			
				<!-- topologie de la cible de déploiement -->
				
				<!--  COMPOSANT SRV JEE -->
				<!-- Cette configuration permet de définir quel est le 
				     serveur jee dans lequel déployer le war buildé.
				-->
				<nom-conteneur-docker-srv-jee>ciblededeploiement-composant-srv-jee</nom-conteneur-docker-srv-e>
				<ip-cible-srv-jee>192.168.1.149</ip-cible-srv-jee>
				<no-port-cible-srv-jee>12546</no-port-cible-srv-jee>
	
				<!--  COMPOSANT SGBDR -->
    
				<!-- Cette configuration pourrait être utilisée pour mettre la BDD dans un état particulier,
				     souhaité pour le déploiement puis laisser un script permettant de faire revenir la BDD 
				     dans son état initial, après la fin des tests.
				-->
				<!-- Si le script de retour à l'état initial n'existe pas, il est créé en
				     utilisant mysqldump -->
				<!-- Si le script de retour à l'état initial existe, il est exécuté pour retour à
				     l'état initial avant déploiement -->
				<!-- <nom-conteneur-docker-sgbdr>ciblededeploiement-composant-sgbdr</nom-conteneur-docker-sgbdr> -->
				<!-- <ip-cible-sgbdr>192.168.1.149</ip-cible-sgbdr> -->
				<!-- <no-port-cible-sgbdr>4466</no-port-cible-sgbdr> -->
				<!-- Utilisateur Linux opérateur du plugin -->
				<lx-user>lauriane</lx-user>
				<lx-pwd>lauriane</lx-pwd>
				<!-- repo git assistant du plugin -->
				<url-repo-git-deploiements>https://github.com/Jean-Baptiste-Lasselle/lauriane-deploiement.git</url-repo-git-deploiements>
				<nom-repo-git-deploiements>lauriane-deploiement</nom-repo-git-deploiements>
				<git-username>Jean-Baptiste-Lasselle</git-username>
				<git-userpwd>***************</git-userpwd>
				<!-- 
				vous devez choisir un répertoire qui pourra être librement utilisé par le plugin maven
				 -->
				<repertoire-repo-git-local>C:\moi\mes_repos_git\tempmvnautobuild2</repertoire-repo-git-local>
						
			</configuration>
	</execution>
    </executions>

# TODO 

Mon eclipse est censé être tout bien configuré pour tester mon appli, il ne reste plus qu'à ajouter tout ce qui relève du datasourde à configurer de 2 manières:

* -  Avec un datasource au niveau serveur, utilisable par 2 applis diférentes (2 qui peuvent être déployées l'une après l'autre par mon plugin...?). Modifications à faire au niveau de mon plugin maven (donc ce repo git), pour peermettre de faire une opération de mise à jour de la configuration du serveur jee. À faire à l'aide de fichiers template remplis avec les infos du datasource, et copiés avec docker cp pour remplacer les fichiers de configuration serveur (standalone.xml pour wildfly, server.xml pour tomcat...)

* - Avec un datasource au niveau de mon application, utilisable par mon application seulement, ps les autres déployées. (là encore, au moins une seconde appli de test à déployer, pour vérifier que'elle n'a effectivement pas accès au datasource). Modifications à faire dans https://github.com/Jean-Baptiste-Lasselle/lauriane-deployeur-test

# TODO: générer le lauriane/config.deployeur.plugin.xml
dans la recette :

https://github.com/Jean-Baptiste-Lasselle/lauriane/releases/tag/v7.3.0



# TODO :

* rendre cohérent les noms de paramètres utilisés pour configurer le plugin, pour les 2 Goals définits (Java et Scala)
* Améliorer l'UX utilisateur du plugin, pour qu'il comprenne bien que le plugin utilise 2 repo Git Assistant: l'un, la référence du code source de l'application, l'autre, le référentiel de déploiements "Infdrastructure As Code". En premier lieu, l'utilisateur doit être informé si sa configuration pom.xml ne mentionne pas 2 repos valides pour les référentiels de versionning et de déploiement.
* Déléguer le travail de déploiement au contrôleur de l'usine logicielle, pour que le plugin ne devinne plus qu'un client de l'usine logicielle.