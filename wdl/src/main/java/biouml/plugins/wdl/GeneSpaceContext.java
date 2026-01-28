package biouml.plugins.wdl;

import java.nio.file.Path;

public class GeneSpaceContext
{
    //Path to all user projects
    protected Path projectDir;
    //Path to worklfows 
    protected Path workflowsDir;
    //Path to supporting data like genome references
    protected Path genomeDir;

    public GeneSpaceContext(Path projectDir, Path workflowsDir, Path genomeDir)
    {
        super();
        this.projectDir = projectDir;
        this.workflowsDir = workflowsDir;
        this.genomeDir = genomeDir;
    }

    public Path getProjectDir()
    {
        return projectDir;
    }

    public void setProjectDir(Path projectDir)
    {
        this.projectDir = projectDir;
    }

    public Path getWorkflowsDir()
    {
        return workflowsDir;
    }

    public void setWorkflowsDir(Path workflowsDir)
    {
        this.workflowsDir = workflowsDir;
    }

    public Path getGenomeDir()
    {
        return genomeDir;
    }

    public void setGenomeDir(Path genomeDir)
    {
        this.genomeDir = genomeDir;
    }

}
