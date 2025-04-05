package com.enit.satellite_platform.modules.project_management.model;

public enum PermissionLevel {
    READ,    // Can view project and image metadata
    EDITOR,  // Can view, add/delete/rename images, edit project details (implies READ)
    WRITE;   // Can do everything EDITOR can, plus share/unshare, delete project (implies EDITOR and READ)

    /**
     * Checks if this permission level includes the required permission level.
     * Assumes a hierarchy: WRITE > EDITOR > READ.
     *
     * @param requiredLevel The level required.
     * @return True if this level grants the required level, false otherwise.
     */
    public boolean includes(PermissionLevel requiredLevel) {
        if (requiredLevel == null) {
            return true; // Or false, depending on desired behavior for null requirement
        }
        // Use compareTo: lower ordinal means less permission
        // this.compareTo(requiredLevel) >= 0 means 'this' has at least the required permission
        return this.compareTo(requiredLevel) >= 0;
    }
}
