package lasselle.deployeur;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
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


@Mojo( name = "deploie")
public class DeploiementJava extends AbstractMojo {
	
	/**
	 * Les paramètres du goal maven
	 */
	@Parameter(alias = "nom-conteneur-docker-srv-jee", property = "nom-conteneur-docker-srv-jee", required = true)
	String nomConteneurDocker= null;
	@Parameter(alias = "ip-cible-srv-jee", property = "ip-cible-srv-jee", required = true)
	String adresseIPcibleDeploiement= null;
	@Parameter(alias = "no-port-cible-srv-jee", property = "no-port-cible-srv-jee", required = true)
	String numeroPortTomcat = null;
	@Parameter(alias = "lx-user", property = "lx-user",defaultValue = "lauriane")
	// aller chercher le username et le pwd
	String SSHusername = null;
	@Parameter(alias = "lx-pwd", property = "lx-pwd", defaultValue = "lauriane")
	String SSHuserpwd = null;

	
	
	/**
	 * Une référence vers le projet lui-même, afind e pouvoir faire mes opérations comme souhaité
	 */
//	@Parameter(readonly = true, defaultValue = "${project}")
//	private MavenProject project;
	@Parameter(defaultValue = "${project.build.directory}")
	private String cheminProjet;
	@Parameter(defaultValue = "${project.artifactId}-${project.version}.war")
	private String nomFichierWAR;
	@Parameter(defaultValue = "${project.artifactId}-${project.version}")
	private String urlContextPathAppli;
	@Parameter(defaultValue = "${project.artifactId}")
	private String urlSHORTContextPathAppli;
	
	
	
	private File fichierWAR = new File(cheminProjet + "/" + nomFichierWAR);
	
	
	/**
	 * Ce plugin utilise un collaborateur:
	 * un repo GIT qui permet le transfert des wars à dployer avec Github.
	 * Un repo Gitlab interne peut aussi être employé.
	 */
	@Parameter(alias = "url-repo-git-deploiements", property = "url-repo-git-deploiements", defaultValue = "https://github.com/Jean-Baptiste-Lasselle/lauriane-deploiement.git")
	private String URL_REPO_GIT_ASSISTANT;
	@Parameter(alias = "nom-repo-git-deploiements", property = "nom-repo-git-deploiements", defaultValue = "lauriane-deploiement")
	private String NOM_REPO_GIT_ASSISTANT; // lauriane-deploiement
	@Parameter(alias = "git-username", property = "git-username", required=true)
	String GITusername = null;
	@Parameter(alias = "git-userpwd", property = "git-userpwd", required=true)
	String GITuserpwd = null;
	
	
	
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
//		// aller chercher la valeur de l'adresse IP de la cible de déploiement
//		String adresseIPcibleDeploiement= null;
//		// La valeur est 
//		// aller chercher le username et le pwd
//		String SSHusername = null;
//		String SSHuserpwd = null;
		
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++++++++	MON PLUGIN	+++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	VALEUR adresseIPcibleDeploiement: " + this.adresseIPcibleDeploiement + " ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	VALEUR SSHusername: " + this.SSHusername + " ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	VALEUR SSHuserpwd: " + this.SSHuserpwd + " ");
		
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	VALEUR cheminProjet: " + this.cheminProjet + " ");
		
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	VALEUR cheminFichierWAR: " + this.nomFichierWAR + " ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	VALEUR cheminProjet + \"/\" + cheminFichierWAR: " + cheminProjet + "/" + nomFichierWAR + " ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" ++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");
		
//		JiblExec.executeCetteCommande("echo \"ah ok d'addaccord\">> voyons.voir ", adresseIPcibleDeploiement, SSHusername, SSHuserpwd);
		// donc après, je n'ai qu'à exécuter mon scp en ligne de commandes tout simplement
		
		copierFihierWarVersCible();
		System.out.println(" +++	COPIE DU WAR FAITE DANS CONTENEUR " + this.nomConteneurDocker);
		executeLeDeploiement();
		System.out.println(" +++	DEPLOIEMENT TERMINE DANS CONTENEUR " + this.nomConteneurDocker);
		System.out.println(" +++	++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++	Votre application est disponible à l'URL:");
		System.out.println(" +++		[http://" + this.adresseIPcibleDeploiement + ":"+ this.numeroPortTomcat + "/" +this.urlContextPathAppli + "]");
		
		
	}
//	@Parameter(alias = "repertoire-repo-git-local", property = "repertoire-repo-git-local", defaultValue = "${project.build.directory}/tempmvnautobuild")
	@Parameter(alias = "repertoire-repo-git-local", property = "repertoire-repo-git-local", required=true)
	String cheminRepoGitLocalDeTravail = null;
	private void copierFihierWarVersCible() {
//		JiblScpTo.faisCopie(this.cheminProjet + "/" + this.nomFichierWAR, this.nomFichierWAR, adresseIPcibleDeploiement, SSHusername, SSHuserpwd);
//		JiblSftp.faisCopie(this.cheminProjet + "/" + this.nomFichierWAR, this.nomFichierWAR, adresseIPcibleDeploiement, SSHusername, SSHuserpwd);
		
		// --
		// Je change de stratégie:
		// 
		// 1./ je commence par faire un commit du war créé par le projet, vers
		//     le repo git "https://github.com/Jean-Baptiste-Lasselle/lauriane-deploiement.git"
		// 
		// => Je créée un nouveau repo git, séparé, et dans C:\moi\mes_repos_git
		File reposDIR = new File("C:\\moi\\mes_repos_git");
		String cheminRepo = this.cheminRepoGitLocalDeTravail;
		File repoDIR = new File(cheminRepo);
		// je le détruis, et le re-créée
		try {
			if (repoDIR.exists()) {
				FileUtils.forceDelete(repoDIR);
			}
		} catch (IOException e2) {
			System.out.println(" JIBL + pb au delete initla du répertoire du repo [" + cheminRepo + "]");
			e2.printStackTrace();
		}
		boolean AETECREE = repoDIR.mkdirs();
		String msgINFOcreationDirRepo = "";
		if (AETECREE) {
			msgINFOcreationDirRepo = " JIBL + le Repertoire de repo a été créé ";
		} else {
			msgINFOcreationDirRepo = "JIBL + le Repertoire de repo N'A PAS été créé";
		}
		
		System.out.println(msgINFOcreationDirRepo );
		
		// CREATION DU REPO
//		try {
////			repoDIR.createNewFile();
//			
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			System.out.println(" ERREUR A LA CREATION DU REPERTOIRE \"" + cheminRepo + "\" ");
//			e.printStackTrace();
//		}
		
		// LE REPO
		Git monrepogit = null;
		// GIT INIT // NON, UN GIT CLONE AU DEPART
		String URLduREPO = this.URL_REPO_GIT_ASSISTANT;
		try {
			CloneCommand cloneCommand = Git.cloneRepository();
			cloneCommand.setDirectory(repoDIR);
			cloneCommand.setURI(URLduREPO);
			cloneCommand.setCredentialsProvider( new UsernamePasswordCredentialsProvider( this.GITusername, this.GITuserpwd ) );
			monrepogit = cloneCommand.call();
//			monrepogit = Git.init().setDirectory(repoDIR).call();
		} catch (IllegalStateException e) {
			System.out.println(" ERREUR AU GIT INIT DANS  \"" + cheminRepo + "\" ");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			System.out.println(" ERREUR AU GIT INIT  DANS  \"" + cheminRepo + "\" ");
			// TODO Auto-generated catch bloc
			e.printStackTrace();
		}
		
		
		// =>> je copie le war dans le repo créé , j'utilise le slsh, plutôt que k'anti-slash, pour la portabilité apportée par la JRE.
//		File aCopier = new File(this.cheminProjet + "\\" + this.nomFichierWAR);
		File aCopier = new File(this.cheminProjet + "/" + this.nomFichierWAR);
		
//		File cibleCopie = new File(repoDIR + "\\" + this.nomFichierWAR);
		File cibleCopie = new File(repoDIR + "/" + this.nomFichierWAR);
		
		try {
			org.apache.commons.io.FileUtils.copyFile(aCopier, cibleCopie);
		} catch (IOException e) {
		    e.printStackTrace();
		}

		// => je fais le add du fichier war
		try {
			DirCache index = monrepogit.add().addFilepattern(this.nomFichierWAR).call();
		} catch (GitAPIException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// => je fais le commit

//		monrepogit.remoteSetUrl(URLduREPO);
//		monrepogit.remoteSetUrl()
		try {
			RevCommit commit = monrepogit.commit().setMessage( "Commit du  deployeur-maven-plugin, pour déploiement vers la cible de déploiement créée par \"lauriane\"" ).call();
		} catch (GitAPIException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// => je clone le repo git "https://github.com/Jean-Baptiste-Lasselle/lauriane-deploiement.git", dans la VM
		
		
		Iterable<PushResult> resultatsPush = null;
		try {
			resultatsPush = monrepogit.push().setCredentialsProvider( new UsernamePasswordCredentialsProvider( this.GITusername, this.GITuserpwd ) ).call();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PushResult pushResult = resultatsPush.iterator().next();
		org.eclipse.jgit.transport.RemoteRefUpdate.Status status = pushResult.getRemoteUpdate( "refs/heads/master" ).getStatus();
		
		
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++   RESULTAT DU PUSH: +++++++++++++ ");
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(status.toString());
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		System.out.println(" +++++++++++++++++++++++++++++++++++++++++++++++ ");
		
		
		// 2./ je  fais le git clone du war que je veux déployer
		//     le repo git "https://github.com/Jean-Baptiste-Lasselle/lauriane-deploiement.git"
		//
		// --
		// je détruis, le repo, et le re-crrées par git clone.
		JiblExec.executeCetteCommande("rm -rf ./"+ this.NOM_REPO_GIT_ASSISTANT, adresseIPcibleDeploiement, SSHusername, SSHuserpwd);
		// 
		JiblExec.executeCetteCommande("git clone \""+ this.URL_REPO_GIT_ASSISTANT + "\"", adresseIPcibleDeploiement, SSHusername, SSHuserpwd);
	}
	private void executeLeDeploiement() {
		
		// je supprime les autres war déployés
		JiblExec.executeCetteCommande("sudo docker exec " + this.nomConteneurDocker + " /bin/bash -c \"rm -rf /usr/local/tomcat/webapps/" + this.urlSHORTContextPathAppli + "*\"", adresseIPcibleDeploiement, SSHusername, SSHuserpwd);
		JiblExec.executeCetteCommande("sudo docker exec " + this.nomConteneurDocker + " /bin/bash -c \"rm -rf /usr/local/tomcat/webapps/" + this.urlSHORTContextPathAppli + "*.war\"", adresseIPcibleDeploiement, SSHusername, SSHuserpwd);
		
		// je copie le war à déployer dans le repertoire webapps
		JiblExec.executeCetteCommande("sudo docker cp "+ this.NOM_REPO_GIT_ASSISTANT + "/"+ this.nomFichierWAR + " " + this.nomConteneurDocker + ":/usr/local/tomcat/webapps", adresseIPcibleDeploiement, SSHusername, SSHuserpwd);
		// je re-démarre le  conteneur entier, au lieu d'un process dans le conteneur.
		JiblExec.executeCetteCommande("sudo docker restart " + this.nomConteneurDocker, adresseIPcibleDeploiement, SSHusername, SSHuserpwd);
		
	}
	

}
