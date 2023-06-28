import argparse
import json


def lamdba_handler(event, context):
    """
        The path of the mesh file and the desired number of viewpoints, which are passed as arguments from the
        command line, are assigned to the variables file and n, respectively. Then the JSON file of the mesh is
        assigned to the variable mesh. The variables mesh_elements and mesh_values are used for a better subdivision of
        the areas of the JSON file. The dictionary dict_viewpoints stores the resulting highest viewpoints and the
        variable idx_ignore stores the indices of the elements that have a higher neighbor and can thus be ignored in
        the iterations of the subsequent FOR loops.
        In the first FOR loop, each element e of the mesh is iteratively examined. If dict_viewpoints contains the desired
        number of viewpoints, the loop is exited. If the current element is already contained in dict_viewpoints or the
        ID of the element is contained in idx_ignore, the element is skipped. In the rest of the loop, the set of nodes,
        the nodes of the current highest neighbors, the height value of the current element, the ID of the current
        highest element, and the height value of the current highest element are assigned to the corresponding
        variables e_nodes, temp_highest_neighbors_nodes, value_e, temp_highest_ID, and temp_highest_value, respectively.
        In the next FOR loop, the current element is iteratively compared with the remaining elements comp_e of the mesh.
        If the element comp_e has the same ID as the element e or the ID of comp_e is contained in idx_ignore, the
        current comp_e is skipped. In the further course of the loop, the ID and the set of nodes of comp_e are assigned
        to the respective variables comp_e_id and comp_e_nodes. If element comp_e is a neighbor of element e or the
        nodes of comp_e are contained in temp_highest_neighbors_nodes, height value of comp_e is assigned to variable
        value_comp_e. If the height value of comp_e is higher than temp_highest_value, the corresponding temporary
        variables are updated and the index of comp_e is added to idx_ignore. If the value is lower than
        temp_highest_value, the index of comp_e is added to idx_ignore. Finally, in the second FOR loop,
        temp_highest_ID is added to dict_viewpoints with the corresponding temp_highest_value. After all elements from
        the mesh have been examined in the first FOR loop or the loop has been exited early, dict_viewpoints is sorted
        and returned as a list.
        :param mesh_file: JSON-File, which contains the mesh
        :param n: Number of desired view points
        :return: List of n view points with ID as key and height as value ordered by highest to lowest value.
    """
    try:
        parser = argparse.ArgumentParser()
        parser.add_argument('mesh', type=str, help="Path to Mesh List to be analysed")
        parser.add_argument('numberOfViewpoints', type=int, help='Number of view spots')
        args = parser.parse_args()
        file = args.mesh
        n = args.numberOfViewpoints
        with open(file) as mesh_file:
            mesh = json.load(mesh_file)
            mesh_elements = mesh['elements']
            mesh_values = mesh['values']
            dict_viewpoints = {}
            idx_ignore = set()

            for idx_e, e in enumerate(mesh_elements):
                if len(dict_viewpoints) == n:
                    break
                if e['id'] in dict_viewpoints or e['id'] in idx_ignore:
                    continue

                e_nodes = set(e['nodes'])
                temp_highest_neighbors_nodes = e_nodes
                value_e = mesh_values[idx_e]['value']
                temp_highest_ID = e['id']
                temp_highest_value = value_e

                for idx_comp_e, comp_e in enumerate(mesh_elements):
                    if comp_e['id'] == e['id'] or comp_e['id'] in idx_ignore:
                        continue

                    comp_e_id = comp_e['id']
                    comp_e_nodes = set(comp_e['nodes'])

                    if not e_nodes.isdisjoint(comp_e_nodes) or \
                            (not comp_e_nodes.isdisjoint(temp_highest_neighbors_nodes)):
                        value_comp_e = mesh_values[idx_comp_e]['value']

                        if value_comp_e > temp_highest_value:
                            temp_highest_value = value_comp_e
                            temp_highest_ID = comp_e_id
                            temp_highest_neighbors_nodes = set(comp_e['nodes'])
                            idx_ignore.add(idx_comp_e)
                        else:
                            idx_ignore.add(idx_comp_e)

                dict_viewpoints[temp_highest_ID] = temp_highest_value

        sorted_viewpoints = dict(sorted(dict_viewpoints.items(), key=lambda x: x[1], reverse=True))
        final_viewlist = []
        for element, value in sorted_viewpoints.items():
            temp_dict = {element: value}
            final_viewlist.append(temp_dict)
        return json.dumps(final_viewlist, indent=2)
    except (Exception, KeyboardInterrupt) as e:
        return 'Error occurred'
