/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.internal.macro.script;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.script.ScriptMacro;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Listens to {@link org.xwiki.script.event.ScriptEvaluatingEvent} and cancels the evaluation if the script
 * is nested.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Component("nestedscriptmacrovalidator")
public class NestedScriptMacroValidatorListener extends AbstractScriptCheckerListener
{
    /**
     * Used to find the type of a Macro defined by a Macro Marker block; we're interested to prevent nested scripts only
     * in Script macros.
     */
    @Requirement
    private MacroManager macroManager;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return "nestedscriptmacrovalidator";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractScriptCheckerListener#check(CancelableEvent, MacroTransformationContext, ScriptMacroParameters)
     */
    @Override
    protected void check(CancelableEvent event, MacroTransformationContext context, ScriptMacroParameters parameters)
    {
        // Traverse the XDOM tree up to the root
        if (context.getCurrentMacroBlock() != null) {
            MacroMarkerBlock parent = context.getCurrentMacroBlock().getParentBlockByType(MacroMarkerBlock.class);
            while (parent != null) {
                String parentId = parent.getId();
                try {
                    Macro< ? > macro = this.macroManager.getMacro(new MacroId(parentId));
                    if (macro instanceof ScriptMacro) {
                        event.cancel("Nested scripts are not allowed");
                    } else if (macro instanceof NestedScriptMacroEnabled) {
                        // This macro has the right to produce script macro whatever the parent.
                        return;
                    } else if ("include".equals(parentId)) {
                        // Included documents intercept the chain of nested script macros with XWiki syntax
                        // TODO: find cleaner way. I don't think we can make include macro depends on script macro to
                        // use NestedScriptMacroEnabled, we should maybe find something more generic
                        return;
                    }
                } catch (MacroLookupException exception) {
                    // Shouldn't happen, the parent macro was already successfully executed earlier
                }
                parent = parent.getParentBlockByType(MacroMarkerBlock.class);
            }
        }
    }
}
