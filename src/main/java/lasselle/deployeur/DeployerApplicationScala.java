package lasselle.deployeur;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
//                                     F‌ileRepositoryBuilder
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import lasselle.ssh.operations.elementaires.JiblExec;
import lasselle.ssh.operations.elementaires.JiblExecSansFin;

//import lasselle.ssh.operations.elementaires.JiblExec;
/**
 * Une recette de déploiement de l'applciation Scala, à exécuter après avoir exécuté le goal [deployeur:provision-scala] cf. {@see MonterCibleDeploiementScala}
 * 
 * Ce goal supprime la version précédente de l'application, pour déployer et démarrer la nouvelle version de l'application
 * 
 *  
 * Tout ce que ce goal fait, c'est exécuter [recette-deploiement-application-scala.sh]
 * ********************************************************************************************************************************
 * Récaitualtif des paramètres:
 * 
 * ********************************************************************************************************************************
 * 
 * => @Parameter(alias = "repertoire-code-scala", property = "repertoire-code-scala", required = true, defaultValue = "scala")
 * => @Parameter(alias = "nom-conteneur-docker-srv-scala", property = "nom-conteneur-docker-srv-scala", required = false)
 * => @Parameter(alias = "ip-cible-srv-scala", property = "ip-cible-srv-scala", required = true)
 * => @Parameter(alias = "no-port-cible-srv-scala", property = "no-port-cible-srv-scala", required = true)
 * => @Parameter(alias = "ops-lx-user", property = "ops-lx-user", defaultValue = "lauriane", required = true)
 * => @Parameter(alias = "ops-lx-pwd", property = "ops-lx-pwd", defaultValue = "lauriane", required = true)
 * => @Parameter(alias = "url-repo-git-app-scala", property = "url-repo-git-app-scala", required = true, defaultValue = "https://github.com/Jean-Baptiste-Lasselle/siteweb-usinelogicielle.com")
 * => @Parameter(alias = "nom-repo-git-app-scala", property = "nom-repo-git-app-scala", defaultValue = "siteweb-usinelogicielle.com")
 * => @Parameter(alias = "url-repo-git-deploiements", property = "url-repo-git-deploiements", defaultValue = "https://github.com/Jean-Baptiste-Lasselle/deploiement-usine-logicielle.com")
 * => @Parameter(alias = "nom-repo-git-deploiements", property = "nom-repo-git-deploiements", defaultValue = "deploiement-usine-logicielle.com")
 * => @Parameter(alias = "ops-git-username", property = "ops-git-username", required = true)
 * => @Parameter(alias = "ops-git-userpwd", property = "ops-git-userpwd", required = true)
 * 
 * 
 * ********************************************************************************************************************************
 *  <repertoire-code-scala></repertoire-code-scala>
 *  <nom-conteneur-docker-srv-scala></nom-conteneur-docker-srv-scala>  (optionnel, pas de valeur par défaut, ainsi, si pointe vers null, alors cela signifie que le déplpoiement ne doit pas se faire dans un conteneur).
 *  <ip-cible-srv-scala></ip-cible-srv-scala>
 *  <no-port-cible-srv-scala></no-port-cible-srv-scala>
 *  <ops-lx-user></ops-lx-user>
 *  <ops-lx-pwd></ops-lx-pwd>
 *  <ops-git-username></ops-git-username>
 *  <ops-git-userpwd></ops-git-userpwd> n'existe plus
 *  <ops-scm-git-username></ops-scm-git-username>
 *  <ops-scm-git-username-pwd></ops-scm-git-pwd> n'existe plus
 *  <url-repo-git-app-scala></url-repo-git-app-scala>
 *  <nom-repo-git-app-scala></nom-repo-git-app-scala>
 *  <url-repo-git-deploiements></url-repo-git-deploiements>
 *  <nom-repo-git-deploiements></nom-repo-git-deploiements>
 *  
 * 
 * 
 * 
 * 
 * 
 * 
 * ********************************************************************************************************************************
 * 
 * 
 * @author Jean-Baptiste Lasselle
 *
 */
@Mojo(name = "deploy-scala-app")
public class DeployerApplicationScala extends AbstractMojo implements ComposantDePipeLineScala {

	/**
	 * ********************************************************************************************************************************
	 * Les paramètres du goal maven
	 * ********************************************************************************************************************************
	 */

	@Parameter(alias = "repertoire-code-scala", property = "repertoire-code-scala", required = true, defaultValue = "scala")
	String nomRepertoireScala = null;
	
	/**
	 * Permet à l'utilisateur du plugin de préciser une liste de valeurs à passer à l'exécution de l'applciation Scala
	 * 
	 *  <exec-args-scala-app></exec-args-scala-app>
	 */
	@Parameter(alias = "exec-args-scala-app", property = "exec-args-scala-app", required = false, defaultValue = "")
	String execArgsScalaApp = null;
	/**
	 * Pas de valeur par défaut, ainsi, si pointe vers null, alors cela signifie que le déplpoiement ne doit pas se faire dans un conteneur. 
	 */
	@Parameter(alias = "nom-conteneur-docker-srv-scala", property = "nom-conteneur-docker-srv-scala", required = false)
	String nomConteneurDocker = null;
	@Parameter(alias = "ip-cible-srv-scala", property = "ip-cible-srv-scala", required = true)
	String adresseIPcibleDeploiement = null;
	@Parameter(alias = "no-port-cible-srv-scala", property = "no-port-cible-srv-scala", required = true)
	String numeroPortSrvScala = null;

	/**
	 * L'opérateur système qui va procéder aux opérations dans la cible de
	 * déploiement
	 */
	@Parameter(alias = "ops-lx-user", property = "ops-lx-user", defaultValue = "lauriane", required = true)
	String ops_lx_username = null;
	@Parameter(alias = "ops-lx-pwd", property = "ops-lx-pwd", defaultValue = "lauriane", required = true)
	String ops_lx_userpwd = null;

	/**
	 * Ce plugin permet de déployer une application scala dont le code source se
	 * trouve versioné par le repo de'URL
	 * {@see DeploiementScala#URL_REPO_CODE_SOURCE_APP_SCALA}
	 * 
	 * Le nom de ce repo est {@see DeploiementScala#NOM_REPO_CODE_SOURCE_APP_SCALA}
	 */
	@Parameter(alias = "url-repo-git-app-scala", property = "url-repo-git-app-scala", required = true, defaultValue = "https://github.com/Jean-Baptiste-Lasselle/siteweb-usinelogicielle.com")
	private String URL_REPO_CODE_SOURCE_APP_SCALA;
	@Parameter(alias = "nom-repo-git-app-scala", property = "nom-repo-git-app-scala", defaultValue = "siteweb-usinelogicielle.com")
	private String NOM_REPO_CODE_SOURCE_APP_SCALA; // lauriane-deploiement

	/**
	 * L'opérateur git qui va procéder aux opérations sur le repo (assistant) de deploiement
	 */
	@Parameter(alias = "ops-git-username", property = "ops-git-username", required = true)
	String ops_git_username = null;
	/**
	 * N'est pas un paramètre: il ne DOIT PAS figurer dans les pom.xml, au risque de
	 * versionner un mot de passe, tros gros risque de sécurité pour els utilisateurs.
	 */
//	@Parameter(alias = "ops-git-userpwd", property = "ops-git-userpwd", required = true)
	String ops_git_userpwd = null;
	
	/**
	 * L'opérateur git qui va procéder aux opérations sur le repo de versionning du code source de l'application Scala
	 */
	@Parameter(alias = "ops-scm-git-username", property = "ops-scm-git-username", required = true)
	String ops_scm_git_username = null;
	/**
	 * N'est pas un paramètre: il ne DOIT PAS figurer dans les pom.xml, au risque de
	 * versionner un mot de passe, tros gros risque de sécurité pour els utilisateurs.
	 */
//	@Parameter(alias = "ops-git-userpwd", property = "ops-git-userpwd", required = false)
	String ops_scm_git_userpwd = null;

	/**
	 * ********************************************************************************************************************************
	 * Quelques références vers des répertoires du projet lui-même, afin de pouvoir
	 * faire les opérations comme souhaité
	 * ********************************************************************************************************************************
	 */
	// @Parameter(readonly = true, .... defaultValue = "mavaleur" .... etc...)
	@Parameter(defaultValue = "${project.basedir}")
	private String cheminRacineProjet;
	@Parameter(defaultValue = "${project.basedir}/scala")
	private String repertoireScala = this.cheminRacineProjet + this.nomRepertoireScala;

	
	@Parameter(defaultValue = "${project.build.directory}")
	private String cheminRepBuildMaven = null;

	/**
	 * Le répertoire dans lequel le code sclala est déployé dans la cible de déploiement
	 */
	@Parameter(alias = "repertoire-deploiement-scala", property = "repertoire-deploiement-scala", required = true)
	private String repertoireAppScalaDsCible;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++  MONTEE CIBLE DEPLOIEMENT SCALA	+++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	         CHECK UP DES VALEURS PARAMETRES            +++");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	VALEUR adresseIPcibleDeploiement: " + this.adresseIPcibleDeploiement + " ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	VALEUR this.ops_lx_username: " + this.ops_lx_username + " ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	VALEUR this.ops_lx_userpwd: " + this.ops_lx_userpwd + " ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		

		/**
		 * 0. Demander interactive des credentials, pour assurer que les données de
		 * sécurité ne soient jamais présentes dans le référentiel de versionning du pom.xml
		 */
		this.demanderMotDePasseRepoGitCodeSource();
//		this.demanderMotDePasseRepoGitAssistantDeploiements();
		/**
		 * 1. Initialiser le positionnement de version du code source de l'app. dans le répertoire {@see DeploiementScala#repertoireScala}
		 */
		this.initialiserCodeSource();
		/**
		 * 2.Je fais le commit and push vers le repo référentiel de versionning du code source de l'application Scala
		 *  
		 */
		
		this.faireCommitAndPushCodeSource();
		
		/**
		 * 3. Je fais le commit and push vers le repo référentiel de versionning des déploiements de l'applciations Scala
		 */
//		this.faireCommitAndPushDeploiement();
		JiblExec.executeCetteCommande(" echo \"DEPLOY SCALA \\>\\> PRESSEZ LA TOUCHE ENTREE DE VOTRE CLAVIER POUR DEMARRER LE DEPLOIEMENT DE L'APPLICATION SCALA\";", adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		/**
		 * 4. Si un process nommé "software-factory" existe, je le stoppe, pour le re-démarrer
		 * Cela me permet de recommencer " de zéro" à partir de la nouvelle version de code.
		 */
		String URI_REPO_RECETTES = "https://github.com/Jean-Baptiste-Lasselle/lauriane";
		String nomFichierTemporaire = "lesnumerospidsbt";
		JiblExec.executeCetteCommande("rm -f ./" + nomFichierTemporaire + ";", adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		JiblExec.executeCetteCommande("ps -ef | grep '[s]oftware-factory'| awk '{print $2}' >> " + nomFichierTemporaire + ";", adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		JiblExec.executeCetteCommande("while read pidprocesssbt; do  kill -9 $pidprocesssbt; done < " + nomFichierTemporaire + ";", adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		JiblExec.executeCetteCommande("rm -f ./" + nomFichierTemporaire + ";", adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		/**
		 * 5. Avec JSch je réalise l'exécution de la recette de montée de la cible de déploiement
		 * 
		 *  => Très important à noter:
		 *  
		 * TODO: l'utilisateur linux qui doit éxécuter cette recette de déploiement est l'utilisateur "comissioner".
		 * Ce doit être un utiloisateur différent de l'utilisateur linux  que le deployeur-plugin utilise.
		 */
		String nomRepertoireOperations = "$HOME/deploiements-app-scala";
		JiblExec.executeCetteCommande("rm -rf " + nomRepertoireOperations, adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		JiblExec.executeCetteCommande("mkdir -p " + nomRepertoireOperations, adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
//		JiblExec.executeCetteCommande("cp " + nomRepertoireOperations, adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		
		
		JiblExec.executeCetteCommande("git clone \"" + URI_REPO_RECETTES + "\" " + nomRepertoireOperations + "/ ;", adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		JiblExec.executeCetteCommande("chmod +x " + nomRepertoireOperations + "/recette-deploiement-application-scala.sh;", adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		// J'exécute la recette de déploiement spécifique à l'OS de la cible de déploiement.
		// Je dois exécuter "sans fin", parce que le process s'exéctant suite à la commande sbt ~run ne se termine jamais.
		JiblExec.executeCetteCommande(" echo \"DEPLOY SCALA - FIN\";", adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		JiblExecSansFin.executeCetteCommande("pwd;" + nomRepertoireOperations + "/recette-deploiement-application-scala.sh " + REPERTOIRE_PROCHAIN_BUILD + " " + this.repertoireAppScalaDsCible + " " + this.execArgsScalaApp + ";", adresseIPcibleDeploiement, this.ops_lx_username, this.ops_lx_userpwd);
		
		/**
		 * 6. Je fais un petit affichage récapitulatif
		 * 
		 */
		this.afficherRecapitulatif();
		
	}

	/**
	 * Pour ne pas écrire de mot de passe dans la configuration du plugin
	 * TODO: évolution qui permet au développeur de gérer les credentials utilisés par le plugin, et d'intégrer cette gestion à des outils de gestions globaux système.
	 * @return le mot de passe à utiliser 
	 * @throws MojoExecutionException lorsque le mot de passe saisi est null ou la chaîne de caractères vide
	 */
	private void demanderMotDePasseRepoGitCodeSource() throws MojoExecutionException {
		this.ops_scm_git_userpwd = this.demanderMotDePassePrRepoGit(this.ops_scm_git_username, this.URL_REPO_CODE_SOURCE_APP_SCALA);
	}
	
	/**
	 * Cette méthode st appelée afin de demander interactivement à l'utilisateur un nom d'utilisateur et un mot de passe pour l'authentificiation à un repo Git.
	 * @param username
	 * @param URL_DU_REPO
	 * @return
	 * @throws MojoExecutionException
	 */
	private String demanderMotDePassePrRepoGit(String username, String URL_DU_REPO) throws MojoExecutionException {
		String motdepasse = null;
		
		motdepasse = JOptionPane.showInputDialog("Quel est le mot de passe de " + "[" + this.ops_git_username + "]" + " pousser sur le repo " + "[" + URL_DU_REPO + "]" + " ?",
				null);
		if (!(motdepasse != null && motdepasse.length() >= 1)) {
			StringBuilder message1 = new StringBuilder();
			String sautLigne = System.getProperty("line.separator");
			
			message1.append("Pour versionner (commit & push) le code source édité");
			message1.append(sautLigne);
			message1.append("dans le repository git " + "[" + URL_DU_REPO + "]");
			message1.append(sautLigne);
			message1.append(" L'utilisateur git: "+ "[" + username + "]" +") ");
			message1.append(sautLigne);
			message1.append(" a saisit un mot de passe null ou de longueur strictement inférieure à 1 ");
			message1.append(sautLigne);
			message1.append("La chaîne de caractère vide et null ne sont pas acceptés par le DEPLOYEUR-MAVEN-PLUGIN ");
			message1.append(sautLigne);
			message1.append("en tant que mot de passe pour une authentification.");
			message1.append(sautLigne);
			
			throw new MojoExecutionException(message1.toString());
		}
		
		
		
		return motdepasse;
		
	}

	
	private String demanderMessageDeCommitAndPushVersRepoGit(String username, String URL_DU_REPO) throws MojoExecutionException {
		String messageDeCommitUtilisateur = null;
		StringBuilder messageDeCommitprepapre = new StringBuilder();
		
		messageDeCommitUtilisateur = JOptionPane.showInputDialog("Saisissez le message de commit & push pour le code source de l'application. Si vous ne saisissez aucun message et cliquez \"OK\", un message de commit par défaut sera généré. " + " - repo: " + "[" + URL_DU_REPO + "]",
				null);
		if (!(messageDeCommitUtilisateur != null && messageDeCommitUtilisateur.length() >= 1)) { /// message par défaut du commit par le [deployeur-maven-plugin]
//			StringBuilder message1 = new StringBuilder();
			String sautLigne = System.getProperty("line.separator");
			messageDeCommitprepapre.append("Commit du  deployeur-maven-plugin, pour déploiement de l'application ");
			messageDeCommitprepapre.append(sautLigne);
			messageDeCommitprepapre.append("[" + this.URL_REPO_CODE_SOURCE_APP_SCALA + "]");
			messageDeCommitprepapre.append(sautLigne);
			messageDeCommitprepapre.append(" déploiement réalisé par l'utilisateur linux \" "+ this.ops_git_username + "\" dans la cible de déploiement.");
			messageDeCommitprepapre.append(sautLigne);
			
//			throw new MojoExecutionException(messageDeCommitprepapre.toString());
			return messageDeCommitprepapre.toString();
		}
		return messageDeCommitUtilisateur;
		
	}

	/**
	 * 6. Je fais un petit affichage récapitulatif:
	 * 
	 * 			DEPLOIEMENT SCALA TERMINE
	 * 
	 * 			L'application scala est disponible à l'url: [http://etc...etc...]
	 * 
	 * 			Votre code source scala dans le répertoire {@see DeploiementScala#repertoireScala} a été poussé vers son repo de versionning: [this.urlRepoCodeSourceAppScala]
	 * 
	 * 
	 * 
	 */
	private void afficherRecapitulatif() {
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++	      MONTEE CIBLE DEPLOIEMENT SCALA TERMINE     ++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	La cible de déploiement est maintenant en service.");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	L'appliscala est disponible à l'url: [http://" + this.adresseIPcibleDeploiement + ":" + this.numeroPortSrvScala);
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		
	}

	private void initialiserCodeSource() throws MojoExecutionException {
		
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++	 INITIALISATION CODE SOURCE  ++++++++++++++ ");
		System.out.println(" ++++++++++++++++	 APPLICATION SCALA   		 ++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		/**
		 * 1. Vérifier si un code source versionné se trouve déjà dans le répertoire {@see DeploiementScala#repertoireScala}
		 */
		String sautLigne = System.getProperty("line.separator");
		try {
			this.verifierSiRepoGitPresent(); // s'il n'est pas présent, une RepoCodeSourceAbsentException est levée 
			// init du repo git pour le code source de l'application scala
			this.initialiserRepoLocalExistantCodeSourceScala();
			
			
			StringBuilder messageTerminalInitialisationCodeSource =  new StringBuilder();
			messageTerminalInitialisationCodeSource.append("DEPLOYEUR-MAVEN-PLUGIN");
			messageTerminalInitialisationCodeSource.append(sautLigne);
			messageTerminalInitialisationCodeSource.append(" l'initialisation du code source s'est déroulée sans exception.");
			messageTerminalInitialisationCodeSource.append(sautLigne);
			System.out.println(messageTerminalInitialisationCodeSource);
			return;
		} catch (RepoCodeSourceAbsentException e) { // sinon, je l'initialise en détruisant le répertoire, 
			/** 
			 * 2. Si aucun repo git n'est trouvé, je l'intialise avec le repo de code source que je clone.
			 *    => Je demande à l'utilisateur de confirmer qu'il veut que je chekout de tel repo de code source
			 *    => s'il réponds non, alors il a un affichge d'un messge qui propose à l'utilisateur de faire
			 *       le clone lui-même manuellement s'il le souhaite.
			 *    
			 */
			JOptionPane.showInputDialog("Enter username@hostname",
					System.getProperty("user.name") + "@localhost");
			

			
			StringBuilder messageTerminalNePasCloner =  new StringBuilder();
			messageTerminalNePasCloner.append("DEPLOYEUR-MAVEN-PLUGIN");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append(" - Le répertoire " + "[" + this.repertoireScala + "]" + "");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append("   ne contient pas de repository git valide.");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append("   1./ Clonez le repository de versionnning de votre");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append("   application manuellement dans" + "[" + this.repertoireScala + "]");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append("   2./ indiquez l'URL du repo Git de votre application scala dans ");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append("   votre pom.xml, dans la configuration d'exécution de votre plugin, avec");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append("   la balise <repo-git-app-scala></repo-git-app-scala>  ");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append("   3./ indiquez le nom  du repo Git de votre application scala dans ");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append("   votre pom.xml, dans la configuration d'exécution de votre plugin, avec");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append("   la balise <nom-repo-git-app-scala></nom-repo-git-app-scala>  ");
			messageTerminalNePasCloner.append(sautLigne);
			messageTerminalNePasCloner.append("DEPLOYEUR-MAVEN-PLUGIN");
			messageTerminalNePasCloner.append(sautLigne);
			
			
			StringBuilder confirmezGitClone = new StringBuilder();			
			confirmezGitClone.append(sautLigne);
			confirmezGitClone.append("Votre répertoire [] ne contient pas de repo git initialisé. ");
			confirmezGitClone.append(sautLigne);
			confirmezGitClone.append("[]");
			confirmezGitClone.append(sautLigne);
			confirmezGitClone.append("ne contient pas de repo git initialisé. ");
			confirmezGitClone.append(sautLigne);
			confirmezGitClone.append(sautLigne);
			
			confirmezGitClone.append("Votre pom.xml précise que le repo de code source de votre application scala est :");
			confirmezGitClone.append(sautLigne);
			confirmezGitClone.append("[]");
			confirmezGitClone.append(sautLigne);
			confirmezGitClone.append("Souhaitez-vous cloner le repo [] dans [], pour reprendre le développement de votre application? ");
			confirmezGitClone.append(sautLigne);
			confirmezGitClone.append("Si vous répondez oui, le répertoire  " + "[" + this.repertoireScala + "]" + " sera détruit");
			confirmezGitClone.append(sautLigne);
			confirmezGitClone.append("s'il existe, et re-créé, pour cloner la dernière version de al branche master de " + "[" + this.URL_REPO_CODE_SOURCE_APP_SCALA + "]");
			confirmezGitClone.append(sautLigne);
			
			
			int confirmationGitClone = JOptionPane.showConfirmDialog(null, confirmezGitClone.toString());
			switch (confirmationGitClone) {
				case JOptionPane.YES_OPTION: {
					// faire le git clone
					this.initialiserRepogitLocalCodeSourceAppliScalaParGitClone();
					break;
				}
				case JOptionPane.NO_OPTION: {
					// faire un affichage puis interrompe le traitement
//					String sautLigne = System.getProperty("line.spearator");

					
					JOptionPane.showMessageDialog(null, messageTerminalNePasCloner.toString());
					// J'interromps le build en lançant une exception
					/**
					 * throw a MojoExecutionException if the problem makes it impossible
					 * to continue with the build, and use the MojoFailureException otherwise.
					 */
					throw new MojoExecutionException(messageTerminalNePasCloner.toString());
//					return;
//					break;
				}
				case JOptionPane.CANCEL_OPTION: {
					// J'interromps le build en lançant une exception
					/**
					 * throw a MojoExecutionException if the problem makes it impossible
					 * to continue with the build, and use the MojoFailureException otherwise.
					 */
					throw new MojoExecutionException(messageTerminalNePasCloner.toString());
//					break;
				}
				default: {
					// J'interromps le build en lançant une exception
					/**
					 * throw a MojoExecutionException if the problem makes it impossible
					 * to continue with the build, and use the MojoFailureException otherwise.
					 */
					throw new MojoExecutionException("Une exception est survenue pendant l'initialisation du code source de l'application Scala.");
//					break;
				}
			}
			
		} /* catch (IOException e) {
			System.out.println(" UNE ERREUYR I/O initialiserCodeSource() après avoir détecté un repo existant ");
			e.printStackTrace();
		} catch (URISyntaxException e) {
			System.out.println(" UNE ERREUYR URISyntaxException initialiserCodeSource() après avoir détecté un repo existant ");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */
		
	}
	
	/**
	 * Pas encorre certain d'utiliser une synchronisation.
	 * TODO: vérifier si le repo local existant est désynchronisé avec el repo distant.
	 * Si désynchrionisé", le repo local doit être détriut et re-cré avec 
	 * {@see DeploiementScala#initialiserRepogitLocalCodeSourceAppliScalaParGitClone() }
	 * 
	 * Pourquoi? Par ce que le repo distant indiqué par le développeur est considéré comme la référence
	 * de versionnig. 
	 */
	private void synchroniserRepoLocalExistantCodeSourceScala() {
		/**
		 * TODO: vérifier si le repo local existant est désynchronisé avec el repo distant.
		 * Si désynchrionisé", le repo local doit être détriut et re-cré avec 
		 * {@see DeploiementScala#initialiserRepogitLocalCodeSourceAppliScalaParGitClone() }
		 * 
		 * Pourquoi? Par ce que le repo distant indiqué par le développeur est considéré comme la référence
		 * de versionnig. 
		 * 
		 */
		
	}
	private Git repoGitLocalCodeSourceScala = null;
	private Git repoGitLocalDeploiementScala = null;
	
	/**
	 * Cette méthode intiialise un repo
	 * pour faire un git pull ensuite.
	 * @throws IOException Lorsque le répertoire {@see DeploiementScala#repertoireScala} n'existe pas, ou pose un problème I/O
	 * @throws URISyntaxException Lorsque l'URL {@see DeploiementScala#URL_REPO_CODE_SOURCE_APP_SCALA } n'est pas une URL valide pour un repo Git
	 * @throws GitAPIException Lorsque la commande d'ajout du repo "remote" a posé un problème
	 */
	private void configurerLeRemoteRepoDuCodeSourceAppScala() throws IOException, URISyntaxException, GitAPIException {
//		StoredConfig config = git.getRepository().getConfig();
//		config.setString("remote", "origin", "url", "http://github.com/user/repo");
//		config.save();
		// TODO Auto-generated method stub
		RemoteAddCommand remoteAddCommand = this.repoGitLocalCodeSourceScala.remoteAdd();
	    remoteAddCommand.setName("origin");
	    remoteAddCommand.setUri(new URIish(this.URL_REPO_CODE_SOURCE_APP_SCALA));
	    remoteAddCommand.call();
	}
	
	/**
	 * Permet d'intialiser le repo dans le cas où il est déjà existant (le code source a été modifié entre 2 exécutions du goal maven).
	 * Cette méthode intialise par un objet non null, le champs {@see DeploiementScala#repoGitLocalCodeSourceScala }
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GitAPIException
	 * @throws MojoExecutionException Le Build est interommpu si l'initialisation du repo a échouée.
	 */
	private void initialiserRepoLocalExistantCodeSourceScala() throws MojoExecutionException {
		
		/**
		 *  j'initialise une instance Git à partir du repo existant 
		 */
		try {
			this.repoGitLocalCodeSourceScala = Git.init().setDirectory(new File(this.repertoireScala)).call();
		//		repoCodeSrcAppScala.remoteSetUrl();
			} catch (IllegalStateException e2) {
		//		e2.printStackTrace();
				throw new MojoExecutionException(" Problème IllegalStateException à l'intialisation du repo local " + "[" + this.repertoireScala + "]" +  " #initialiserRepoLocalExistantCodeSourceScala()", e2);
			} catch (GitAPIException e2) {
		//		e2.printStackTrace();
				throw new MojoExecutionException(" Problème GitAPIException à l'intialisation du repo local "+ "[" + this.repertoireScala + "]" +  " #initialiserRepoLocalExistantCodeSourceScala()", e2);
			}
		
		/**
		 *  je configure le remote.
		 */
//		this.configurerLeRemoteRepoDuCodeSourceAppScala();
		
		/**
		 * Je synchronisee le code osurce local aec le repo distant, référence de versionning.
		 */
//		this.synchroniserRepoLocalExistantCodeSourceScala();
	}

	/**
	 * Réalise le commit and push du code source édité, veeers le repo de code source de l'application
	 * @throws MojoExecutionException
	 */
	private void faireCommitAndPushCodeSource() throws MojoExecutionException {
		
		File repertoireDeTravail = new File(this.repertoireScala);
		Git repoCodeSrcAppScala = null;
		// Je fais le git init, s'il un repo est déjà intiailisé et que nosu sommes en
		// train de travailler, alors aucune modification n'est apportée au repo local git
		// Simplement fait pour récupérer une référence Java sur le repository local
//		try {
//			repoCodeSrcAppScala = Git.init().setDirectory(repertoireDeTravail).call();
////			repoCodeSrcAppScala.remoteSetUrl();
//		} catch (IllegalStateException e2) {
////			e2.printStackTrace();
//			throw new MojoExecutionException(" Problème à l'intialisation du repo local " + "[" + this.repertoireScala + "]" +  ", avant de faire le commit and push du code source de l'application vers son repo de versionning de code source.", e2);
//		} catch (GitAPIException e2) {
////			e2.printStackTrace();
//			throw new MojoExecutionException(" Problème à l'intialisation du repo local "+ "[" + this.repertoireScala + "]" + ", avant de faire le commit and push du code source de l'application vers son repo de versionning de code source.", e2);
//		}
		
		// à la place du git init, je récupère simlement le repo git initialisé par la méthode {@see DeploiementSclala#initialiserLeCodeSource() }
		repoCodeSrcAppScala = this.repoGitLocalCodeSourceScala; // il est censé être déjà initialisé
		System.out.println(" [>Le champs this.repoGitLocalCodeSourceScala <] " + (this.repoGitLocalCodeSourceScala != null?" n'est pas NULL": " est NULL"));
		// => je fais le add du fichier war
		try { /// pour ajouter tous les fichiers (soit disant)
			DirCache index = repoCodeSrcAppScala.add().addFilepattern(".").call();
		} catch (GitAPIException e1) {
			throw new MojoExecutionException(" Problème à l'ajout (git add) des fichiers au versionning dans le repo local "+ "[" + this.repertoireScala + "]" + ", avant de faire le commit and push du code source de l'application vers son repo de versionning de code source.", e1);
		}
		
		// => je vérifies le Git status avant le commit and push
		try {
			this.verifierLeStatutDugitRepo(this.repoGitLocalCodeSourceScala);
		} catch (NoWorkTreeException | GitAPIException e2) {
			throw new MojoExecutionException(" Un problème est survenu lorsquele deployeur plugina  essayé de vérifier le status du repo Git versionnant le code source de l'application Scala.", e2);
		}

		// => je fais le commit
		RevCommit commit = null;
		try {
			String messageDeCommit = this.demanderMessageDeCommitAndPushVersRepoGit(this.ops_scm_git_username, this.URL_REPO_CODE_SOURCE_APP_SCALA);
			commit = repoCodeSrcAppScala.commit().setMessage(messageDeCommit).call();
//			commit.get
		} catch (GitAPIException e1) {
			// TODO Auto-generated catch block
			throw new MojoExecutionException(" Problème au COMMIT dans le repo local "+ "[" + this.repertoireScala + "]", e1);
		}
		// => je clone le repo git "https://github.com/Jean-Baptiste-Lasselle/lauriane-deploiement.git", dans la VM
		
		
		Iterable<PushResult> resultatsPush = null;
		try {
			resultatsPush = repoCodeSrcAppScala.push().setRemote(this.URL_REPO_CODE_SOURCE_APP_SCALA).setCredentialsProvider( new UsernamePasswordCredentialsProvider( this.ops_scm_git_username, this.ops_scm_git_userpwd ) ).call();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			throw new MojoExecutionException(" Problème au PUSH du repo local " + "[" + this.repertoireScala + "]" + " vers " + "[" + this.URL_REPO_CODE_SOURCE_APP_SCALA + "]", e);
		}
		PushResult pushResult = resultatsPush.iterator().next();
		org.eclipse.jgit.transport.RemoteRefUpdate.Status status = pushResult.getRemoteUpdate( "refs/heads/master" ).getStatus();
		
		
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++   RESULTAT DU PUSH vers : " + "[" + this.URL_REPO_CODE_SOURCE_APP_SCALA + "]" +" +++++++++++++ ");
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++   code retour du PUSH : " + status.toString());
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++   commit id : " + commit.getId());
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++   commit time : " + commit.getCommitTime());
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++   commit message : " + commit.getShortMessage());
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
				
		
		;
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
	}
	
	/**
	 * Cette méthode intitialise le repo local du code source de l'application Scala, dans le cas où le rpeo  ne soit pas encore initialisé
	 * repo git pas encore intialisé <=> {@see DeploiementScala#verifierSiRepoGitPresent() } lève une excecption
	 * 
	 * Cette méthode attribue une instance {@see org.eclipse.jgit.api.Git } "non nulle" (ne pointant pas vers "null"), à
	 * {@see DeploiementScala#repoGitLocalCodeSourceScala }
	 * 
	 * 
	 */
	private void initialiserRepogitLocalCodeSourceAppliScalaParGitClone() {
		// --
		File repCodeSrcScala = new File(this.repertoireScala);
//		String cheminRepo = this.cheminRepoGitLocalDeTravail;
//		File repoDIR = new File(cheminRepo);
		// je le détruis, et le re-créée
		try {
			if (repCodeSrcScala.exists()) {
				FileUtils.forceDelete(repCodeSrcScala);
			}
		} catch (IOException e2) {
			System.out.println(" JIBL + pb au delete initla du répertoire du repo [" + this.repertoireScala + "]");
			e2.printStackTrace();
		}
		boolean AETECREE = repCodeSrcScala.mkdirs();
		String msgINFOcreationDirRepo = "";
		if (AETECREE) {
			msgINFOcreationDirRepo = " JIBL + le Repertoire de repo a été créé ";
		} else {
			msgINFOcreationDirRepo = "JIBL + le Repertoire de repo N'A PAS été créé";
		}

		System.out.println(msgINFOcreationDirRepo);
		// --
		
		
		
		// LE REPO
		Git repoGitAppliScala = null;
//		repCodeSrcScala
		// GIT INIT // NON, UN GIT CLONE AU DEPART
		String URLduREPO = this.URL_REPO_CODE_SOURCE_APP_SCALA;
		try {
			CloneCommand cloneCommand = Git.cloneRepository();
			cloneCommand.setDirectory(repCodeSrcScala);
			cloneCommand.setURI(URLduREPO);
			cloneCommand.setCredentialsProvider(
					new UsernamePasswordCredentialsProvider(this.ops_git_username, this.ops_git_userpwd));
			this.repoGitLocalCodeSourceScala = cloneCommand.call();
			// repoGitAppliScala = Git.init().setDirectory(repoDIR).call();
		} catch (IllegalStateException e) {
			System.out.println(" ERREUR AU GIT INIT DANS  \"" + this.repertoireScala + "\" ");
			e.printStackTrace();
		} catch (GitAPIException e) {
			System.out.println(" ERREUR AU GIT INIT  DANS  \"" + this.repertoireScala + "\" ");
			// TODO Auto-generated catch bloc
			e.printStackTrace();
		}

		
	}

	/**
	 * ----------------------------------------------------------------------------------
	 * Classes d'Exceptions utilisées pour l'initilisatgion du code source à éditer.
	 * ----------------------------------------------------------------------------------
	 *
	 */
	
	
	private static class RepoAbsentException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2366627317074190637L;

		public RepoAbsentException() {
			super();
			// TODO Auto-generated constructor stub
		}

		public RepoAbsentException(String message, Throwable cause, boolean enableSuppression,
				boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
			// TODO Auto-generated constructor stub
		}

		public RepoAbsentException(String message, Throwable cause) {
			super(message, cause);
			// TODO Auto-generated constructor stub
		}

		public RepoAbsentException(String message) {
			super(message);
			// TODO Auto-generated constructor stub
		}

		public RepoAbsentException(Throwable cause) {
			super(cause);
			// TODO Auto-generated constructor stub
		}

	}

	/**
	 * Levée dans le cas où mon plugin ne trouve pas le repo de déploiement
	 * compagnon
	 * 
	 * @author Jean-Baptiste Lasselle
	 *
	 */
	private static class RepoDeploiementAbsentException extends RepoAbsentException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -2248874288255295215L;
		private static String MESSAGE = "Le repo de déploiement n'est pas précisé dans la configuration d'exécution du goal <goal>deploie-app-scala</goal> dans votre pom.xml";

		public RepoDeploiementAbsentException() {
			super();
			// TODO Auto-generated constructor stub
		}

		public RepoDeploiementAbsentException(String message, Throwable cause, boolean enableSuppression,
				boolean writableStackTrace) {
			super(MESSAGE, cause, enableSuppression, writableStackTrace);
			// TODO Auto-generated constructor stub
		}

		public RepoDeploiementAbsentException(String message, Throwable cause) {
			super(MESSAGE, cause);
			// TODO Auto-generated constructor stub
		}

		public RepoDeploiementAbsentException(String message) {
			super(MESSAGE);
			// TODO Auto-generated constructor stub
		}

		public RepoDeploiementAbsentException(Throwable cause) {
			super(cause);
			// TODO Auto-generated constructor stub
		}

	}

	/**
	 * Levée dans le cas où mon plugin ne trouve pas
	 * le repo local du code source de l'application scala.
	 * 
	 * dans {@see DeploiementScala#repertoireScala}
	 * 
	 * @author Jean-Baptiste Lasselle
	 *
	 */
	public static class RepoCodeSourceAbsentException extends RepoAbsentException {

		private static String MESSAGE = "Le repo de code source Scala n'a pas été touvé dans le répertoire que vous avez précisé dans votre pom.xml, pour configurer le DEPLOYEUR plugin dans la balise <repertoire-code-scala></repertoire-code-scala>.";

		public RepoCodeSourceAbsentException() {
			super();
			// TODO Auto-generated constructor stub
		}

		public RepoCodeSourceAbsentException(String message, Throwable cause, boolean enableSuppression,
				boolean writableStackTrace) {
			super(MESSAGE, cause, enableSuppression, writableStackTrace);
			// TODO Auto-generated constructor stub
		}

		public RepoCodeSourceAbsentException(String message, Throwable cause) {
			super(MESSAGE, cause);
			// TODO Auto-generated constructor stub
		}

		public RepoCodeSourceAbsentException(String message) {
			super(MESSAGE);
			// TODO Auto-generated constructor stub
		}

		public RepoCodeSourceAbsentException(Throwable cause) {
			super(cause);
			// TODO Auto-generated constructor stub
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -1263975271738160187L;

	}

	/**
	 * Perrmet de vérifer que le repo git du code source de l'application scala est
	 * bien intialisé dans le répertoire {@see DeploiementScala#repertoireScala}
	 */
	private void verifierSiRepoGitPresent() throws RepoCodeSourceAbsentException {
		// si le répertoire ne contient pas de répertoire ".git", alors exception
		File repertoirePointGit = new File(this.repertoireScala + "/.git/");
		if (!(repertoirePointGit.exists() && repertoirePointGit.isDirectory())) {
			throw new RepoCodeSourceAbsentException();
		}

		/**
		 * si la commande "git status" renvoie :
		 * 
		 * fatal: Not a git repository (or any of the parent directories): .git
		 * 
		 * alors le répertoire ne contient pas de répertoire ".git", alors exception
		 */

		//

	}

	/**
	 * Si cette méthode ne lève pas d'aexception, c'est que le repo est valide, et Git est bien installé....
	 * 
	 * @throws NoWorkTreeException
	 * @throws GitAPIException
	 */
	private void verifierLeStatutDugitRepo(Git repoAverifier) throws NoWorkTreeException, GitAPIException {
		Status status = repoAverifier.status().call();
		Set<String> fichiersAjoutes = status.getAdded();
		Iterator<String> iterateur = fichiersAjoutes.iterator();
		
		StringBuilder affichage = new StringBuilder();
		String sautLigne = System.getProperty("line.separator");
		
		affichage.append(" Fichiers ajoutés pour le prochain commit:");
		String nomFichier = null;
		while(iterateur.hasNext()) {
			nomFichier = iterateur.next();
			affichage.append(" ajouté*: " + nomFichier);
			affichage.append(sautLigne);
		}
		System.out.println(affichage);
	}
}
