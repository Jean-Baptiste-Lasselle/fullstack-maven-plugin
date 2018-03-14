package lasselle.deployeur; // lasselle.deployeur.InitialisationFullStackScala

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ThresholdingOutputStream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import lasselle.ssh.operations.elementaires.JiblExec;

/**
 * TODO: Après avoir généré le project maven à partir de l'archetype maven 'fullstack-scala-archetype' (à crééer et versionner avec un repo github), Ce goal:
 *  
 *  => Il s'agit d'initialiser le cycle de développement Fullstack en partant:
 *   	+++ pour le code source l'application Scala, d'une version précise d'un repo Git pour lequel nous avons un accès en lecture seule.
 *   	+++ pour le code source la recette de montée de la cible de déploiement de l'application Scala, d'une version précise d'un repo Git pour lequel nous avons un accès en lecture seule.
 *   	+++ pour le code source la recette de déploiement de l'application Scala, d'une version précise d'un repo Git pour lequel nous avons un accès en lecture seule.
 *  
 *  Pour chacun des 3 codes sources, des plugins eclipse spécialisés pourl'édition dans le language du code source peuvent être installés
 *  
 *  Pour chacun des 3 codes sources, il faudra donc préciser un paramètre de configuration dans le pom.xml:
 *   	+++ pour le code source l'application Scala: <url-repo-git-app-scala>https://github.com/Jean-Baptiste-Lasselle/siteweb-usinelogicielle.com</url-repo-git-app-scala>
 *   	+++ pour le code source la recette de montée de la cible de déploiement de l'application Scala: <url-repo-git-recette-montee-cible-deploiement-app-scala>https://github.com/Jean-Baptiste-Lasselle/siteweb-usinelogicielle.com</url-repo-git-recette-montee-cible-deploiement-app-scala>
 *   	+++ pour le code source la recette de déploiement de l'application Scala:  <url-repo-git-recette-deploiement-app-scala>https://github.com/Jean-Baptiste-Lasselle/siteweb-usinelogicielle.com</url-repo-git-recette-deploiement-app-scala>
 *   
 *  
 * @author Jean-Baptiste Lasselle
 *
 */
@Mojo(name = "scala-fullstack-init")
public class InitialisationFullStackScala extends AbstractMojo {

	/**
	 * ********************************************************************************************************************************
	 * Les paramètres du goal maven
	 * ********************************************************************************************************************************
	 */

	@Parameter(alias = "repertoire-code-scala", property = "repertoire-code-scala", required = true, defaultValue = "scala")
	String nomRepertoireScala = null;

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
	 * Ce plugin permet de déployer uen application scala dont le code source se
	 * trouve versioné par le repo de'URL
	 * {@see DeploiementScala#URL_REPO_CODE_SOURCE_APP_SCALA}
	 * 
	 * Le nom de ce repo est {@see DeploiementScala#NOM_REPO_CODE_SOURCE_APP_SCALA}
	 */
	@Parameter(alias = "repo-git-app-scala", property = "repo-git-app-scala", required = true, defaultValue = "https://github.com/Jean-Baptiste-Lasselle/siteweb-usinelogicielle.com")
	private String URL_REPO_CODE_SOURCE_APP_SCALA;
	private String urlRepoCodeSourceAppScala = null;
	@Parameter(alias = "nom-repo-git-app-scala", property = "nom-repo-git-app-scala", defaultValue = "siteweb-usinelogicielle.com")
	private String NOM_REPO_CODE_SOURCE_APP_SCALA; // lauriane-deploiement

	/**
	 * Ce plugin utilise un collaborateur: un repo GIT qui permet le transfert de
	 * l'artefact à déployer avec Github. Un repo Gitlab interne peut aussi être
	 * employé.
	 */
	@Parameter(alias = "url-repo-git-deploiements", property = "url-repo-git-deploiements", defaultValue = "https://github.com/Jean-Baptiste-Lasselle/deploiement-usine-logicielle.com")
	private String URL_REPO_GIT_ASSISTANT;
	@Parameter(alias = "nom-repo-git-deploiements", property = "nom-repo-git-deploiements", defaultValue = "deploiement-usine-logicielle.com")
	private String NOM_REPO_GIT_ASSISTANT; // lauriane-deploiement

	/**
	 * L'opérateur git qui va procéder aux opérations sur le repo de deploiement
	 */
	@Parameter(alias = "ops-git-username", property = "ops-git-username", required = true)
	String ops_git_username = null;
	@Parameter(alias = "ops-git-userpwd", property = "ops-git-userpwd", required = true)
	String ops_git_userpwd = null;

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


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		
		this.initialiserCodeSource();
	}

	private void initialiserCodeSource() throws MojoExecutionException {
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++++++++	INITIALISATION SCALA	+++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	         CHECK UP DES VALEURS PARAMETRES            +++");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	VALEUR adresseIPcibleDeploiement: " + this.adresseIPcibleDeploiement + " ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		
		
		/**
		 * 1. Vérifier si un code source versionné se trouve déjà dans le répertoire {@see DeploiementScala#repertoireScala}
		 */
		String sautLigne = System.getProperty("line.separator");
		try {
			this.verifierSiRepoGitPresent();
			// Si c'est le cas, il n'y a rien à faire.
			StringBuilder messageTerminalInitialisationCodeSource =  new StringBuilder();
			messageTerminalInitialisationCodeSource.append("DEPLOYEUR-MAVEN-PLUGIN");
			messageTerminalInitialisationCodeSource.append(sautLigne);
			System.out.println(messageTerminalInitialisationCodeSource);
			return;
		} catch (RepoCodeSourceAbsentException e) { // sinon, je l'initialise en détruisant le répertoire, 
			/** 
			 * 2. Si aucun repo git n'est trouvé, je l'intialise avec le repo de code source que je clone.
			 *    => Je demande à l'utilisateur de confirmer q'il veut que je chekout de tel repo de code source
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
					this.clonerRegpoCodeSourceAppliScala();
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
					throw new MojoExecutionException(messageTerminalNePasCloner.toString());
//					break;
				}
			}
		}
		
	}

	private void clonerRegpoCodeSourceAppliScala() {
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
			repoGitAppliScala = cloneCommand.call();
			// monrepogit = Git.init().setDirectory(repoDIR).call();
		} catch (IllegalStateException e) {
			System.out.println(" ERREUR AU GIT INIT DANS  \"" + this.repertoireScala + "\" ");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			System.out.println(" ERREUR AU GIT INIT  DANS  \"" + this.repertoireScala + "\" ");
			// TODO Auto-generated catch bloc
			e.printStackTrace();
		}

		
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
	 * Levée dans le cas où mon plugin ne trouve pas le repo de code source scala
	 * dans {@see DeploiementScala#repertoireScala}
	 * 
	 * @author Jean-Baptiste Lasselle
	 *
	 */
	private static class RepoCodeSourceAbsentException extends RepoAbsentException {

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

	
}
