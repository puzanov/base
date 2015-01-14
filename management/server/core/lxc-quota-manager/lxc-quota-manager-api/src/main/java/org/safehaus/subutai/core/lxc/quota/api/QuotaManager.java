package org.safehaus.subutai.core.lxc.quota.api;


import java.util.Set;
import java.util.UUID;

import org.safehaus.subutai.common.quota.DiskPartition;
import org.safehaus.subutai.common.quota.DiskQuota;
import org.safehaus.subutai.common.quota.PeerQuotaInfo;
import org.safehaus.subutai.common.quota.QuotaInfo;
import org.safehaus.subutai.common.quota.QuotaType;


/**
 * Created by talas on 10/7/14.
 */
public interface QuotaManager
{
    /**
     * Set Quota for container specified with parameters passed parameter as value to change, it is enum and specific
     * value specified in enum key and newValue can be in any format for setting new value. host is a host with
     * collection of container we intend to modify
     */
    public void setQuota( String containerName, QuotaInfo quota ) throws QuotaException;

    /**
     * Get specified quota of container
     */
    public PeerQuotaInfo getQuota( String containerName, QuotaType quotaType ) throws QuotaException;


    /**
     * Returns RAM quota on container in megabytes
     *
     * @param containerId - id of container
     *
     * @return - quota in mb
     */
    public int getRamQuota( UUID containerId ) throws QuotaException;

    /**
     * Sets RAM quota on container in megabytes
     *
     * @param containerId - id of container
     * @param ramInMb - quota in mb
     */
    public void setRamQuota( UUID containerId, int ramInMb ) throws QuotaException;


    /**
     * Returns CPU quota on container in percent
     *
     * @param containerId - id of container
     *
     * @return - cpu quota on container in percent
     */
    public int getCpuQuota( UUID containerId ) throws QuotaException;

    /**
     * Sets CPU quota on container in percent
     *
     * @param containerId - id of container
     * @param cpuPercent - cpu quota in percent
     */
    public void setCpuQuota( UUID containerId, int cpuPercent ) throws QuotaException;

    /**
     * Returns allowed cpus/cores ids on container
     *
     * @param containerId - id of container
     *
     * @return - allowed cpu set
     */
    public Set<Integer> getCpuSet( UUID containerId ) throws QuotaException;

    /**
     * Sets allowed cpus/cores on container
     *
     * @param containerId - id of container
     * @param cpuSet - allowed cpu set
     */
    public void setCpuSet( UUID containerId, Set<Integer> cpuSet ) throws QuotaException;


    public DiskQuota getDiskQuota( UUID containerId, DiskPartition diskPartition ) throws QuotaException;

    public void setDiskQuota( UUID containerId, DiskQuota diskQuota ) throws QuotaException;
}
