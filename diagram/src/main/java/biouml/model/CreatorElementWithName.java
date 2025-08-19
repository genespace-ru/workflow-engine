package biouml.model;

import java.awt.Point;

import javax.annotation.Nonnull;

public interface CreatorElementWithName extends SemanticController
{
    /**
     * Creates new instance of <code>DiagramElement</code> with specific kernel type, name and properties
     * without using any interface dialogs.
     */
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, String name, Point point, Object properties);
}
