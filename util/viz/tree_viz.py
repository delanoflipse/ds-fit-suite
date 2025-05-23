from graphviz import Digraph
from dataclasses import dataclass, field, replace
import re


@dataclass
class SearchNode:
    id: str
    index: int
    mode: str
    signature: str
    uid: str
    children: list['SearchNode'] = field(default_factory=list)

def simplify_signature(sig: str) -> str:
    signature = str(sig)
    signature = re.sub(r"\(.+\)", "", signature)
    signature = re.sub(r"(GET|POST|PUT|DELETE) ", "", signature)
    signature = re.sub(r"\{.*\}", "", signature)
    signature_parts = signature.split("/")
    if len(signature_parts) > 1:
        return ".../" + signature_parts[-1]
    return signature


def simplify_name(name: str):
    no_fault = name.replace("Fault", "")
    no_brackets = no_fault.replace("[", "").replace("]", "")
    if no_brackets == "":
        return "&empty;", "", ""

    no_uid = no_brackets.replace("uid=", "")
    if ", mode=" not in no_uid:
        return no_uid, "", ""
    uid, mode = no_uid.split(", mode=")

    signature = ""
    uid_parts = uid.split(">")
    relevant_parts = []
    for i in range(len(uid_parts)):
        p_uid = re.sub(r"\{.*\}", "", uid_parts[i])
        p1, count = p_uid.split("#")
        if ":" in p1:
            destination, signature = p1.split(":")
        else:
            destination = p1
        name = f"{destination}"
        is_relevant = i == len(uid_parts) - 1
        if count != "0":
            is_relevant = True
            name += f"#{count}"

        if is_relevant:
            relevant_parts.append(name)

    uid_str = ">\n".join(relevant_parts)

    mode = mode.replace("HTTP_ERROR(", "")
    mode = mode.replace(")", "")

    return uid_str, mode, simplify_signature(signature)


def get_node_label(node: SearchNode, needs_signature=False):
    parts = [node.uid]
    if needs_signature and node.signature:
        parts.append(node.signature)
    if node.mode:
        parts.append(node.mode)
    return "\n".join(parts)

def id_to_indices(id: str)-> list[int]:
    indices = [int(x) for x in id.split(",")]
    return indices

def get_edge_label(node: SearchNode):
    indices = [int(x) for x in node.id.split(",")]
    if len(indices) == 1:
        return f"{node.index}"

    min_id = min(indices)
    max_id = max(indices)
    return f"{min_id} - {max_id} ({len(indices)})"

def combine_tree(node: SearchNode):
    # Group children by key
    grouped_by_key: dict[str, list[SearchNode]] = {}
    for child in node.children:
        key = f"{child.uid}\n{child.signature}" if needs_signature else child.uid
        if key not in grouped_by_key:
            grouped_by_key[key] = []
        grouped_by_key[key].append(child)
    
    new_children: list[SearchNode] = []
    for key, children in grouped_by_key.items():
        combined_id = ",".join([c.id for c in children])
        indices = [c.index for c in children]
        combined_index = min(indices)
        combined_mode = ""
        combined_children = []

        for child in children:
            combined_children.extend(child.children)

        combined_child = SearchNode(
            id=combined_id,
            index=combined_index,
            mode=combined_mode,
            signature=children[0].signature,
            uid=children[0].uid,
            children=combined_children,
        )
        new_children.append(combine_tree(combined_child))
    
    new_node = replace(node)
    new_node.children = new_children
    return new_node

def render_tree_structure(dot: Digraph, node: SearchNode, needs_signature=False):
    node_label = get_node_label(node, needs_signature)
    dot.node(node.id, label=node_label)

    for child in node.children:
        render_tree_structure(dot, child, needs_signature)
        dot.edge(node.id, child.id, label=get_edge_label(child))

def parse_tree(tree: dict) -> tuple[SearchNode, list[SearchNode]]:
    nodes: list[SearchNode] = []

    if isinstance(tree, dict):
        uid, mode, signature = simplify_name(tree['node'])
        children: list[SearchNode] = []

        for child in tree.get('children', []):
            child_node, child_nodes = parse_tree(child)
            children.append(child_node)
            nodes.append(child_node)
            nodes.extend(child_nodes)

        node = SearchNode(
            id=str(tree['index']),
            index=tree['index'],
            mode=str(mode),
            signature=signature,
            uid=uid,
            children=children
        )

        nodes.append(node)
        return node, nodes
    else:
        raise ValueError("Invalid tree structure")


def needs_signature(nodes: list[SearchNode]):
    for node in nodes:
        for other in nodes:
            if node.uid == other.uid and node.signature != other.signature:
                return True
    return False


def render_tree(tree: dict, output_name: str, combine=True):
    root, nodes = parse_tree(tree)
    use_sig = needs_signature(nodes)

    if combine:
        root = combine_tree(root)

    dot = Digraph(comment='Faultspace Search', format='pdf')
    render_tree_structure(dot, root, use_sig)
    dot.render(filename=output_name)
