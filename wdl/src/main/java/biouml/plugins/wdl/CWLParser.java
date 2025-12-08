package biouml.plugins.wdl;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.ApplicationUtils;

public class CWLParser
{
    private Diagram diagram;

    public Diagram loadDiagram(File f, DataCollection dc, String name) throws Exception
    {
        String content = ApplicationUtils.readAsString( f );
        Diagram diagram = new WDLDiagramType().createDiagram( dc, name, new DiagramInfo( null, name ) );
        return loadDiagram( content, diagram );
    }

    public Diagram loadDiagram(String content, Diagram diagram) throws Exception
    {
        this.diagram = diagram;
        diagram.clear();
        Map<String, Object> map = parseYaml( content );
        List<Object> graph = getList( map, "$graph" );
        for( Object element : graph )
        {
            processElement( element );
        }
        addRelations();
        return diagram;
    }

    public void processElement(Object element) throws Exception
    {
        if( element instanceof Map )
        {
            Map<String, Object> map = (Map)element;
            String clazz = getString( map, "class" );
            switch( clazz )
            {
                case "Workflow":
                {
                    processWorkflow( map );
                    break;
                }
                case "CommandLineTool":
                {
                    processCommandLineTool( map );
                    break;
                }
            }
        }
    }

    public void processCommandLineTool(Map<String, Object> process) throws Exception
    {
        String id = getString( process, "id" );

        Object baseCommand = process.get( "baseCommand" );
        List arguments = getList( process, "arguments" );

        Map<String, Object> inputs = getMap( process, "inputs" );
        Map<String, Object> outputs = getMap( process, "outputs" );
        id = DefaultSemanticController.generateUniqueName( diagram, id );
        Compartment task = new Compartment( diagram, new Stub( null, id, WDLConstants.TASK_TYPE ) );
        task.setNotificationEnabled( false );
        WorkflowUtil.setCommand( task, processCommand( baseCommand, arguments ) );
        int inputCount = 0;
        for( String inputName : inputs.keySet() )
        {
            Map<String, Object> input = getMap( inputs, inputName );
            String type = getString( input, "type", "" );
            String portId = DefaultSemanticController.generateUniqueName( diagram, inputName );
            Node port = WDLImporter.addPort( portId, WDLConstants.INPUT_TYPE, inputCount, task );
            WorkflowUtil.setName( task, inputName );
            WorkflowUtil.setType( port, toBioUMLType( type ) );
            WorkflowUtil.setExpression( port, "" );//TODO: default values
            inputCount++;
        }
        int outputCount = 0;
        for( String outputName : outputs.keySet() )
        {
            Map<String, Object> output = getMap( outputs, outputName );
            String type = getString( output, "type", "" );
            String expression = getString( output, "outputBinding", "" );
            String portId = DefaultSemanticController.generateUniqueName( diagram, outputName );
            Node port = WDLImporter.addPort( portId, WDLConstants.OUTPUT_TYPE, outputCount, task );
            WorkflowUtil.setName( task, outputName );
            WorkflowUtil.setType( port, toBioUMLType( type ) );
            WorkflowUtil.setExpression( port, toBioUMLExpression( expression ) );
            outputCount++;
        }
        int maxPorts = Math.max( inputCount, outputCount );
        int height = Math.max( 50, 24 * maxPorts + 8 );
        task.setShapeSize( new Dimension( 200, height ) );
        task.getAttributes().add( new DynamicProperty( "innerNodesPortFinder", Boolean.class, true ) );
        task.setNotificationEnabled( true );
        diagram.put( task );
    }

    public String processCommand(Object baseCommand, List<Object> arguments) throws Exception
    {
        //special case
        if( arguments.size() == 2 && arguments.get( 0 ).equals( "-c" ) && arguments.get( 1 ) instanceof Map )
        {
            if( baseCommand.equals( "sh" ) )
                return ( (Map)arguments.get( 1 ) ).get( "valueFrom" ).toString();
            else
                return baseCommand.toString() + " " + ( (Map)arguments.get( 1 ) ).get( "valueFrom" ).toString();
        }
        String args = StreamEx.of( arguments ).joining( " " );
        if( baseCommand instanceof List )
            baseCommand = StreamEx.of( (List)baseCommand ).joining( " " );
        return baseCommand + " " + args;
    }

    public void processWorkflow(Map<String, Object> workflow) throws Exception
    {
        processInputs( workflow );
        processOutputs( workflow );
        processSteps( workflow );
    }

    public void processInputs(Map<String, Object> element)
    {
        Map<String, Object> inputs = getMap( element, "inputs" );
        for( String key : inputs.keySet() )
        {
            Map<String, Object> input = getMap( inputs, key );
            String type = getString( input, "type", "" );
            createExpression( diagram, type, key, "", WDLConstants.WORKFLOW_INPUT_TYPE );
        }
    }

    public void processOutputs(Map<String, Object> element)
    {
        Map<String, Object> outputs = getMap( element, "outputs" );
        for( String key : outputs.keySet() )
        {
            Map<String, Object> output = getMap( outputs, key );
            String type = getString( output, "type", "" );
            String source = getString( output, "outputSource", "" );
            createExpression( diagram, type, key, source, WDLConstants.WORKFLOW_OUTPUT_TYPE );
        }
    }
    public void processSteps(Map<String, Object> element) throws Exception
    {
        Map<String, Object> steps = getMap( element, "steps" );
        for( String key : steps.keySet() )
        {
            Map<String, Object> step = getMap( steps, key );
            createCall( diagram, key, step );
        }
    }

    public Node createExpression(Compartment parent, String type, String name, String expression, String expressionType)
    {
        String id = DefaultSemanticController.generateUniqueName( diagram, name );
        Node node = new Node( diagram, new Stub( diagram, id, expressionType ) );
        WorkflowUtil.setType( node, toBioUMLType( type ) );
        WorkflowUtil.setName( node, name );
        WorkflowUtil.setExpression( node, toBioUMLExpression( expression ) );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }

    public Compartment createCall(Compartment parent, String name, Map<String, Object> callMap) throws Exception
    {
        Map<String, Object> inMap = getMap( callMap, "in" );
        List<Object> outList = getList( callMap, "out" );
        String taskRef = getString( callMap, "run" );
        if( taskRef.startsWith( "#" ) )
            taskRef = taskRef.substring( 1 );
        
        Compartment task = (Compartment)diagram.findNode( taskRef );
        String id = DefaultSemanticController.generateUniqueName( diagram, name );
        Compartment call = new Compartment( parent, new Stub( null, id, WDLConstants.CALL_TYPE ) );
        call.setShapeSize( new Dimension( 200, 0 ) );
        call.setNotificationEnabled( false );
        WorkflowUtil.setTaskRef( call, taskRef );
        int inputCount = 0;
        for( String inKey : inMap.keySet() )
        {
            String value = inMap.get( inKey ).toString();
            String portId = DefaultSemanticController.generateUniqueName( diagram, name );
            Node port = WDLImporter.addPort( portId, WDLConstants.INPUT_TYPE, inputCount++, call );
            WorkflowUtil.setExpression( port, toBioUMLExpression( value ) );
            WorkflowUtil.setName( port, inKey );
        }
        int outputCount = 0;
        for( Object out : outList )
        {
            if( out instanceof String )
            {
                String value = out.toString();
                String portId = DefaultSemanticController.generateUniqueName( diagram, value );
                Node port = WDLImporter.addPort( portId, WDLConstants.OUTPUT_TYPE, outputCount++, call );
                WorkflowUtil.setName( port, value );
            }
            else
                throw new Exception( "Unknown out type: " + out );
        }

        int maxPorts = Math.max( inputCount, outputCount );
        int height = Math.max( 50, 24 * maxPorts + 8 );
        call.setShapeSize( new Dimension( 200, height ) );
        call.getAttributes().add( new DynamicProperty( "innerNodesPortFinder", Boolean.class, true ) );
        call.setNotificationEnabled( true );
        parent.put( call );
        return call;
    }

    public void addRelations()
    {
        for( Compartment c : WorkflowUtil.getCalls( diagram ) )
        {
            for( Node input : WorkflowUtil.getInputs( c ) )
            {
                addSourceEdges( input );
            }

            for( Node expression : WorkflowUtil.getExpressions( diagram ) )
            {
                addSourceEdges( expression );
            }

            for( Node output : WorkflowUtil.getExternalOutputs( diagram ) )
            {
                addSourceEdges( output );
            }
        }
    }

    public void addSourceEdges(Node node)
    {
        String expression = WorkflowUtil.getExpression( node );
        if( expression.contains( "." ) )
        {
            String[] parts = expression.split( "\\." );
            String sourceCallName = parts[0];
            String sourcePortName = parts[1];
            Node sourceCall = diagram.findNode( sourceCallName );
            Node sourcePort = (Node) ( (Compartment)sourceCall ).get( sourcePortName );
            WDLImporter.createLink( sourcePort, node, WDLConstants.LINK_TYPE );
        }
        else
        {
            Node source = diagram.findNode( expression );
            WDLImporter.createLink( source, node, WDLConstants.LINK_TYPE );
        }
    }

    public static String getString(Map<String, Object> map, String name, String defaultValue)
    {
        Object obj = map.get( name );
        if( obj != null )
            return obj.toString();
        return defaultValue;
    }

    public static String getString(Map<String, Object> map, String name)
    {
        Object obj = map.get( name );
        if( obj != null )
            return obj.toString();
        return null;
    }

    public static Map<String, Object> getMap(Map<String, Object> map, String name)
    {
        Object obj = map.get( name );
        if( obj instanceof Map )
            return (Map)obj;
        return new HashMap<String, Object>();
    }

    public static List<Object> getList(Map<String, Object> map, String name)
    {
        Object obj = map.get( name );
        if( obj instanceof List )
            return (List)obj;
        return new ArrayList<Object>();
    }

    public static String toBioUMLExpression(String cwlExpression)
    {
        String result = cwlExpression.replace( "/", "." );
        return result;
    }

    public static String toBioUMLType(String cwlType)
    {
        if( cwlType.equals( "string" ) )
            return "String";
        return cwlType;
    }

    public static Map<String, Object> parseYaml(String text)
    {
        Yaml parser = new Yaml();

        Object root;
        try
        {
            root = parser.load( text );
        }
        catch( Exception e )
        {
            return null;
        }
        if( ! ( root instanceof Map ) )
            return null;

        Map<?, ?> rootMap = (Map<?, ?>)root;
        System.out.println( rootMap );
        return (Map<String, Object>)rootMap;
    }
}
