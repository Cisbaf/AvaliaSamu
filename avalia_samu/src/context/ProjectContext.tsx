'use client';

import { ProjectProvider as ModularProvider, useProjects as useModularProjects } from './project/ProjectProvider';

export const ProjectProvider = ModularProvider;
export const useProjects = useModularProjects;