package com.subscriptiontracker.entity;

/**
 * Enumeration representing user roles for access control.
 *
 * <p>Roles are used for Role-Based Access Control (RBAC) to determine
 * what actions a user is authorized to perform. The role hierarchy is:</p>
 *
 * <ul>
 *   <li>{@link #USER} - Standard user with access to their own resources</li>
 *   <li>{@link #ADMIN} - Administrative user with access to system-wide operations</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see User
 */
public enum Role {

    /**
     * Standard user role with access to personal subscription management.
     *
     * <p>Users with this role can:</p>
     * <ul>
     *   <li>Manage their own subscriptions and services</li>
     *   <li>View their own payment records</li>
     *   <li>Update their account settings</li>
     * </ul>
     */
    USER,

    /**
     * Administrative role with elevated privileges.
     *
     * <p>Admins have all USER permissions plus:</p>
     * <ul>
     *   <li>View job run history and system status</li>
     *   <li>Manually trigger FX rate refreshes</li>
     *   <li>Access system administration endpoints</li>
     * </ul>
     */
    ADMIN
}
